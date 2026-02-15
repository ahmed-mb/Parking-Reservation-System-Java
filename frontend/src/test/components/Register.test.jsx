import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import Register from '../../components/Register';

// Mock useDemoMode
vi.mock('../../App', () => ({
  useDemoMode: () => ({ demoMode: true, sessionTimeout: 0 })
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderRegister = () => {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <AuthProvider>
        <ModernAlertProvider>
          <Routes>
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('Register', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render registration form', () => {
    renderRegister();
    
    expect(screen.getByText('Register for Parking')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Full Name/)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Email Address/)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Mobile Number/)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Your Address/)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Car Plate Number/)).toBeInTheDocument();
  });

  it('should render CREATE ACCOUNT button', () => {
    renderRegister();
    expect(screen.getByText('CREATE ACCOUNT')).toBeInTheDocument();
  });

  it('should show error when passwords do not match', async () => {
    renderRegister();
    
    const inputs = screen.getAllByRole('textbox');
    const passwordInputs = document.querySelectorAll('input[type="password"]');
    
    fireEvent.change(screen.getByPlaceholderText(/Full Name/), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByPlaceholderText(/Email Address/), { target: { value: 'test@test.com' } });
    fireEvent.change(passwordInputs[0], { target: { value: 'Password@123' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'DifferentPassword@123' } });
    fireEvent.change(screen.getByPlaceholderText(/Mobile Number/), { target: { value: '1234567890' } });
    fireEvent.change(screen.getByPlaceholderText(/Your Address/), { target: { value: '123 Test St' } });
    fireEvent.change(screen.getByPlaceholderText(/Car Plate Number/), { target: { value: 'ABC123' } });
    
    fireEvent.click(screen.getByText('CREATE ACCOUNT'));
    
    await waitFor(() => {
      expect(screen.getByText('Passwords do not match!')).toBeInTheDocument();
    });
  });

  it('should allow typing in all form fields', async () => {
    const user = userEvent.setup();
    renderRegister();
    
    const nameInput = screen.getByPlaceholderText(/Full Name/);
    await user.type(nameInput, 'John Doe');
    expect(nameInput).toHaveValue('John Doe');
    
    const emailInput = screen.getByPlaceholderText(/Email Address/);
    await user.type(emailInput, 'john@example.com');
    expect(emailInput).toHaveValue('john@example.com');
  });

  it('should submit form successfully with matching passwords', async () => {
    renderRegister();
    
    const passwordInputs = document.querySelectorAll('input[type="password"]');
    
    fireEvent.change(screen.getByPlaceholderText(/Full Name/), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByPlaceholderText(/Email Address/), { target: { value: 'newuser@test.com' } });
    fireEvent.change(passwordInputs[0], { target: { value: 'Password@123' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'Password@123' } });
    fireEvent.change(screen.getByPlaceholderText(/Mobile Number/), { target: { value: '1234567890' } });
    fireEvent.change(screen.getByPlaceholderText(/Your Address/), { target: { value: '123 Test St' } });
    fireEvent.change(screen.getByPlaceholderText(/Car Plate Number/), { target: { value: 'ABC123' } });
    
    fireEvent.click(screen.getByText('CREATE ACCOUNT'));
    
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('should render Navbar component', () => {
    renderRegister();
    // Navbar should be rendered - check for nav element
    expect(document.querySelector('nav')).toBeInTheDocument();
  });

  it('should have required fields', () => {
    renderRegister();
    
    const nameInput = screen.getByPlaceholderText(/Full Name/);
    const emailInput = screen.getByPlaceholderText(/Email Address/);
    
    expect(nameInput).toBeRequired();
    expect(emailInput).toBeRequired();
  });
});
