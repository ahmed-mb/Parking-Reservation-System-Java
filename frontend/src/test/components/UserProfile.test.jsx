import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import UserProfile from '../../components/UserProfile';
import { mockUser } from '../mocks/handlers';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderUserProfile = () => {
  localStorage.setItem('token', 'mock-token');
  localStorage.setItem('user', JSON.stringify({ email: 'test@example.com', username: 'testuser', role: 'Customer' }));
  
  return render(
    <MemoryRouter initialEntries={['/profile']}>
      <AuthProvider>
        <ModernAlertProvider>
          <Routes>
            <Route path="/profile" element={<UserProfile />} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('UserProfile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should show loading spinner initially', () => {
    renderUserProfile();
    expect(document.querySelector('.spinner-border')).toBeInTheDocument();
  });

  it('should render profile table after loading', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText('ID')).toBeInTheDocument();
      expect(screen.getByText('Username')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Mobile')).toBeInTheDocument();
      expect(screen.getByText('Address')).toBeInTheDocument();
      expect(screen.getByText('Car Plate')).toBeInTheDocument();
      expect(screen.getByText('Balance Credit')).toBeInTheDocument();
    });
  });

  it('should display user information', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText('testuser')).toBeInTheDocument();
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
      expect(screen.getByText('1234567890')).toBeInTheDocument();
      expect(screen.getByText('123 Test St')).toBeInTheDocument();
      expect(screen.getByText('ABC123')).toBeInTheDocument();
    });
  });

  it('should display welcome message with username', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText(/Welcome testuser/)).toBeInTheDocument();
    });
  });

  it('should have edit button', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      const editButton = screen.getByTitle('Edit Profile');
      expect(editButton).toBeInTheDocument();
    });
  });

  it('should switch to edit mode when edit button is clicked', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByTitle('Edit Profile')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Edit Profile'));
    
    await waitFor(() => {
      // In edit mode, should show save and cancel buttons
      expect(screen.getByTitle('Save Changes')).toBeInTheDocument();
      expect(screen.getByTitle('Cancel')).toBeInTheDocument();
    });
  });

  it('should cancel edit mode when cancel button is clicked', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByTitle('Edit Profile')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Edit Profile'));
    
    await waitFor(() => {
      expect(screen.getByTitle('Cancel')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Cancel'));
    
    await waitFor(() => {
      expect(screen.getByTitle('Edit Profile')).toBeInTheDocument();
    });
  });

  it('should render navigation links', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText('User page')).toBeInTheDocument();
      expect(screen.getByText('History')).toBeInTheDocument();
      expect(screen.getByText('Profile')).toBeInTheDocument();
      expect(screen.getByText('Current Booking')).toBeInTheDocument();
      expect(screen.getByText('Logout')).toBeInTheDocument();
    });
  });

  it('should logout when logout link is clicked', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText('Logout')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Logout'));
    
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('should save profile changes when save button is clicked', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByTitle('Edit Profile')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Edit Profile'));
    
    await waitFor(() => {
      expect(screen.getByTitle('Save Changes')).toBeInTheDocument();
    });
    
    // Modify a field
    const inputs = document.querySelectorAll('input[type="text"]');
    if (inputs.length > 0) {
      fireEvent.change(inputs[0], { target: { value: 'Updated Name' } });
    }
    
    fireEvent.click(screen.getByTitle('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Profile Updated')).toBeInTheDocument();
    });
  });

  it('should handle profile update error', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.put('/api/users/:id', () => {
        return HttpResponse.json({ message: 'Update failed' }, { status: 400 });
      })
    );
    
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByTitle('Edit Profile')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Edit Profile'));
    
    await waitFor(() => {
      expect(screen.getByTitle('Save Changes')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Update Failed')).toBeInTheDocument();
    });
  });

  it('should show empty state when no user info', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json(null);
      })
    );
    
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText('No profile information found')).toBeInTheDocument();
    });
  });

  it('should display credit balance correctly', async () => {
    renderUserProfile();
    
    await waitFor(() => {
      expect(screen.getByText('10.00')).toBeInTheDocument();
    });
  });
});
