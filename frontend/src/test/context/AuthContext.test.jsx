import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../../context/AuthContext';
import axios from 'axios';

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.getItem.mockClear();
    localStorage.setItem.mockClear();
    localStorage.removeItem.mockClear();
  });

  it('should provide default values when not authenticated', async () => {
    localStorage.getItem.mockReturnValue(null);
    
    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
    expect(result.current.isAuthenticated()).toBe(false);
    expect(result.current.isAdmin()).toBe(false);
  });

  it('should load user from localStorage on mount', async () => {
    const mockUser = { email: 'test@example.com', username: 'testuser', role: 'Customer' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(mockUser);
      if (key === 'token') return 'mock-token';
      return null;
    });

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.user).toEqual(mockUser);
    expect(result.current.token).toBe('mock-token');
    expect(result.current.isAuthenticated()).toBe(true);
  });

  it('should set Authorization header when loading from localStorage', async () => {
    const mockUser = { email: 'test@example.com', username: 'testuser', role: 'Customer' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(mockUser);
      if (key === 'token') return 'stored-token';
      return null;
    });

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
    renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(axios.defaults.headers.common['Authorization']).toBe('Bearer stored-token');
    });
  });

  it('should login successfully', async () => {
    localStorage.getItem.mockReturnValue(null);
    
    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const userData = { email: 'test@example.com', username: 'testuser', role: 'Customer' };
    const token = 'new-token';

    act(() => {
      result.current.login(userData, token);
    });

    expect(result.current.user).toEqual(userData);
    expect(result.current.token).toBe(token);
    expect(localStorage.setItem).toHaveBeenCalledWith('user', JSON.stringify(userData));
    expect(localStorage.setItem).toHaveBeenCalledWith('token', token);
    expect(axios.defaults.headers.common['Authorization']).toBe(`Bearer ${token}`);
  });

  it('should logout successfully', async () => {
    const mockUser = { email: 'test@example.com', username: 'testuser', role: 'Customer' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(mockUser);
      if (key === 'token') return 'mock-token';
      return null;
    });

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    act(() => {
      result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
    expect(localStorage.removeItem).toHaveBeenCalledWith('user');
    expect(localStorage.removeItem).toHaveBeenCalledWith('token');
    expect(axios.defaults.headers.common['Authorization']).toBeUndefined();
  });

  it('should identify admin user correctly', async () => {
    const adminUser = { email: 'admin@example.com', username: 'admin', role: 'Admin' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(adminUser);
      if (key === 'token') return 'admin-token';
      return null;
    });

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.isAdmin()).toBe(true);
  });

  it('should throw error when useAuth is used outside AuthProvider', () => {
    // Suppress console.error for this test
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    expect(() => {
      renderHook(() => useAuth());
    }).toThrow('useAuth must be used within an AuthProvider');
    
    consoleSpy.mockRestore();
  });
});
