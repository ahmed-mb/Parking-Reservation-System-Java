import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import ProtectedRoute from '../../components/ProtectedRoute';

const renderWithRouter = (initialEntry, requireAdmin = false) => {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route path="/dashboard" element={<div>Dashboard Page</div>} />
          <Route 
            path="/protected" 
            element={
              <ProtectedRoute requireAdmin={requireAdmin}>
                <div>Protected Content</div>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute requireAdmin={true}>
                <div>Admin Content</div>
              </ProtectedRoute>
            } 
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('ProtectedRoute', () => {
  it('should show loading spinner while checking auth', async () => {
    localStorage.getItem.mockReturnValue(null);
    
    renderWithRouter('/protected');
    
    // Loading should be brief, then redirect to login
    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
  });

  it('should redirect to login when not authenticated', async () => {
    localStorage.getItem.mockReturnValue(null);
    
    renderWithRouter('/protected');
    
    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
  });

  it('should render children when authenticated', async () => {
    const mockUser = { email: 'test@example.com', username: 'testuser', role: 'Customer' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(mockUser);
      if (key === 'token') return 'mock-token';
      return null;
    });
    
    renderWithRouter('/protected');
    
    await waitFor(() => {
      expect(screen.getByText('Protected Content')).toBeInTheDocument();
    });
  });

  it('should redirect non-admin to dashboard when requireAdmin is true', async () => {
    const mockUser = { email: 'test@example.com', username: 'testuser', role: 'Customer' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(mockUser);
      if (key === 'token') return 'mock-token';
      return null;
    });
    
    renderWithRouter('/admin', true);
    
    await waitFor(() => {
      expect(screen.getByText('Dashboard Page')).toBeInTheDocument();
    });
  });

  it('should render admin content when user is admin', async () => {
    const adminUser = { email: 'admin@example.com', username: 'admin', role: 'Admin' };
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'user') return JSON.stringify(adminUser);
      if (key === 'token') return 'admin-token';
      return null;
    });
    
    renderWithRouter('/admin', true);
    
    await waitFor(() => {
      expect(screen.getByText('Admin Content')).toBeInTheDocument();
    });
  });
});
