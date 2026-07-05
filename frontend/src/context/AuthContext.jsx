/**
 * AuthContext.jsx — global authentication state.
 *
 * Holds the current user object and JWT token, persisting both to
 * localStorage so sessions survive page reloads. On mount it hydrates state
 * from localStorage and sets the axios Authorization header when a token
 * is found.
 *
 * Exposes via context: user, token, loading (true until hydration finishes),
 * login(userData, authToken), logout(), isAuthenticated(), and isAdmin()
 * (true when user.role === 'Admin'). Consume with the useAuth() hook, which
 * throws if used outside an AuthProvider.
 */
import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import axios from 'axios';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Load user from localStorage on mount
    const storedUser = localStorage.getItem('user');
    const storedToken = localStorage.getItem('token');
    
    if (storedUser && storedToken) {
      setUser(JSON.parse(storedUser));
      setToken(storedToken);
      axios.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
    }
    setLoading(false);
  }, []);

  const login = (userData, authToken) => {
    setUser(userData);
    setToken(authToken);
    localStorage.setItem('user', JSON.stringify(userData));
    localStorage.setItem('token', authToken);
    axios.defaults.headers.common['Authorization'] = `Bearer ${authToken}`;
  };

  // useCallback with an empty dependency array so this has a stable identity
  // across renders — it only touches state setters and module-level
  // objects (localStorage, axios), none of which are reactive values it
  // needs to close over. Without this, the interceptor effect below (which
  // lists logout as a dependency) would eject and re-register on every
  // single render, per react-hooks/exhaustive-deps.
  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    delete axios.defaults.headers.common['Authorization'];
  }, []);

  // Separate effect (not merged into the hydration effect above) so that
  // `logout` is already initialized when this runs — it closes over it and
  // lists it as a dependency, which would be a temporal-dead-zone error if
  // this ran before the `const logout = ...` above. Registered once and
  // ejected on unmount so remounting AuthProvider (e.g. in tests) never
  // accumulates duplicate interceptors.
  useEffect(() => {
    const interceptorId = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        const status = error.response?.status;
        const url = error.config?.url;
        // A 401 from the login/register endpoints themselves just means bad
        // credentials — not an expired session — so don't wipe storage or
        // redirect (that would blow away the error message the user needs
        // to see). 403 is a real permission error, not an expiry signal.
        const isAuthEndpoint = url === '/api/users/login' || url === '/api/users/register';
        if (status === 401 && !isAuthEndpoint) {
          logout();
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.response.eject(interceptorId);
    };
  }, [logout]);

  const isAuthenticated = () => {
    return !!token;
  };

  const isAdmin = () => {
    return user?.role === 'Admin';
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout, isAuthenticated, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
