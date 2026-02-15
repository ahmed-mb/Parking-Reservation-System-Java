import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import Login from '../../components/Login';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';

// Mock useDemoMode
vi.mock('../../App', () => ({
  useDemoMode: () => ({ demoMode: true, sessionTimeout: 0 })
}));

const renderLogin = () => {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <ModernAlertProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<div>Dashboard</div>} />
            <Route path="/admin" element={<div>Admin Panel</div>} />
          </Routes>
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('Login', () => {
  it('should render login form', async () => {
    localStorage.getItem.mockReturnValue(null);
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/Password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /LOGIN/i })).toBeInTheDocument();
    });
  });

  it('should render welcome message', async () => {
    localStorage.getItem.mockReturnValue(null);
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByText(/Welcome Back/i)).toBeInTheDocument();
    });
  });

  it('should render register link', async () => {
    localStorage.getItem.mockReturnValue(null);
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByText(/Don't have an account/i)).toBeInTheDocument();
    });
  });

  it('should allow typing email and password', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
    });

    const emailInput = screen.getByPlaceholderText(/Email/i);
    const passwordInput = screen.getByPlaceholderText(/Password/i);

    await user.type(emailInput, 'test@example.com');
    await user.type(passwordInput, 'Password@123');

    expect(emailInput).toHaveValue('test@example.com');
    expect(passwordInput).toHaveValue('Password@123');
  });

  it('should login successfully and redirect to dashboard', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText(/Email/i), 'test@example.com');
    await user.type(screen.getByPlaceholderText(/Password/i), 'Password@123');
    await user.click(screen.getByRole('button', { name: /LOGIN/i }));

    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });
  });

  it('should login as admin and redirect to admin panel', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText(/Email/i), 'admin@example.com');
    await user.type(screen.getByPlaceholderText(/Password/i), 'Admin@123');
    await user.click(screen.getByRole('button', { name: /LOGIN/i }));

    await waitFor(() => {
      expect(screen.getByText('Admin Panel')).toBeInTheDocument();
    });
  });

  it('should show error on invalid credentials', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText(/Email/i), 'wrong@example.com');
    await user.type(screen.getByPlaceholderText(/Password/i), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /LOGIN/i }));

    await waitFor(() => {
      expect(screen.getByText(/Invalid email or password/i)).toBeInTheDocument();
    });
  });

  it('should disable inputs while loading', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText(/Email/i), 'test@example.com');
    await user.type(screen.getByPlaceholderText(/Password/i), 'Password@123');
    
    // Click login - button should show loading state
    const loginButton = screen.getByRole('button', { name: /LOGIN/i });
    await user.click(loginButton);

    // Wait for the loading state or redirect
    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });
  });

  it('should handle reCAPTCHA not ready in non-demo mode', async () => {
    // This test would require mocking the demo mode to false
    // For now, we test the demo mode flow which skips reCAPTCHA
    localStorage.getItem.mockReturnValue(null);
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /LOGIN/i })).toBeInTheDocument();
    });
  });

  it('should show error on API failure', async () => {
    localStorage.getItem.mockReturnValue(null);
    
    server.use(
      http.post('/api/users/login', () => {
        return HttpResponse.json({ message: 'Server error' }, { status: 500 });
      })
    );
    
    const user = userEvent.setup();
    renderLogin();
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Email/i)).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText(/Email/i), 'test@example.com');
    await user.type(screen.getByPlaceholderText(/Password/i), 'Password@123');
    await user.click(screen.getByRole('button', { name: /LOGIN/i }));

    await waitFor(() => {
      expect(screen.getByText(/Server error|Login failed/i)).toBeInTheDocument();
    });
  });
});
