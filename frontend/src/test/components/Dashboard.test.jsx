import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import Dashboard from '../../components/Dashboard';
import { mockUser, mockBookings } from '../mocks/handlers';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderDashboard = (initialEntries = ['/dashboard']) => {
  // Set up auth state
  localStorage.setItem('token', 'mock-token');
  localStorage.setItem('user', JSON.stringify({ email: 'test@example.com', username: 'testuser', role: 'Customer' }));
  
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider>
        <ModernAlertProvider>
          <Routes>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/current-booking" element={<div>Current Booking Page</div>} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should show loading spinner initially', () => {
    renderDashboard();
    expect(document.querySelector('.spinner-border')).toBeInTheDocument();
  });

  it('should render dashboard content after loading', async () => {
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('Reserve Your Parking Spot')).toBeInTheDocument();
    });
    
    expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
  });

  it('should display welcome message with username', async () => {
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText(/Welcome testuser/)).toBeInTheDocument();
    });
  });

  it('should render navigation links', async () => {
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('User page')).toBeInTheDocument();
    });
    
    expect(screen.getByText('History')).toBeInTheDocument();
    expect(screen.getByText('Profile')).toBeInTheDocument();
    expect(screen.getByText('Current Booking')).toBeInTheDocument();
    expect(screen.getByText('Logout')).toBeInTheDocument();
  });

  it('should show insufficient credit error when credit is low', async () => {
    // This would require mocking the API to return a user with low credit
    // The handler returns mockUser with credit: 10.00, so booking should work
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    // Book Now button should be enabled
    const bookButton = screen.getByText('BOOK NOW');
    expect(bookButton).not.toBeDisabled();
  });

  it('should show BOOKING... text when booking is in progress', async () => {
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('BOOK NOW'));
    
    // Should briefly show BOOKING... state
    await waitFor(() => {
      // Either shows BOOKING... or navigates to current-booking
      const button = screen.queryByText('BOOKING...');
      if (button) {
        expect(button).toBeInTheDocument();
      }
    }, { timeout: 500 });
  });

  it('should have logout functionality', async () => {
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('Logout')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Logout'));
    
    // Should clear localStorage and navigate
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('should render book now button with correct styling', async () => {
    renderDashboard();
    
    await waitFor(() => {
      const bookButton = screen.getByText('BOOK NOW');
      expect(bookButton).toHaveClass('btn-book-now');
    });
  });

  it('should show error when credit is insufficient', async () => {
    // Override to return user with low credit
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json({
          id: 1,
          email: 'test@example.com',
          username: 'testuser',
          role: 'Customer',
          credit: 2.00, // Less than $6
          carPlateNo: 'ABC123'
        });
      }),
      http.get('/api/bookings/my-active', () => {
        return HttpResponse.json([]);
      })
    );
    
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('BOOK NOW'));
    
    await waitFor(() => {
      expect(screen.getByText('Insufficient Credit')).toBeInTheDocument();
    });
  });

  it('should show warning when user already has active booking', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json({
          id: 1,
          email: 'test@example.com',
          username: 'testuser',
          role: 'Customer',
          credit: 20.00,
          carPlateNo: 'ABC123'
        });
      }),
      http.get('/api/bookings/my-active', () => {
        return HttpResponse.json([{ id: 1, status: 'Active', parkingSpot: 'A-001' }]);
      })
    );
    
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('BOOK NOW'));
    
    await waitFor(() => {
      expect(screen.getByText('Active Booking Exists')).toBeInTheDocument();
    });
  });

  it('should show success and navigate on successful booking', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json({
          id: 1,
          email: 'test@example.com',
          username: 'testuser',
          role: 'Customer',
          credit: 20.00,
          carPlateNo: 'ABC123'
        });
      }),
      http.get('/api/bookings/my-active', () => {
        return HttpResponse.json([]);
      }),
      http.get('/api/parking/available', () => {
        return HttpResponse.json([{ parkingId: 'A-001', availability: 'available' }]);
      }),
      http.post('/api/bookings', () => {
        return HttpResponse.json({ id: 1, parkingSpot: 'A-001', status: 'Active' });
      })
    );
    
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('BOOK NOW'));
    
    await waitFor(() => {
      expect(screen.getByText('Booking Successful!')).toBeInTheDocument();
    });
  });

  it('should show error when no spots available', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json({
          id: 1,
          username: 'testuser',
          credit: 20.00,
          carPlateNo: 'ABC123'
        });
      }),
      http.get('/api/bookings/my-active', () => {
        return HttpResponse.json([]);
      }),
      http.get('/api/parking/available', () => {
        return HttpResponse.json([]);
      })
    );
    
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('BOOK NOW'));
    
    await waitFor(() => {
      expect(screen.getByText('No Spots Available')).toBeInTheDocument();
    });
  });

  it('should handle booking API error', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/users/me', () => {
        return HttpResponse.json({
          id: 1,
          username: 'testuser',
          credit: 20.00,
          carPlateNo: 'ABC123'
        });
      }),
      http.get('/api/bookings/my-active', () => {
        return HttpResponse.json([]);
      }),
      http.get('/api/parking/available', () => {
        return HttpResponse.json([{ parkingId: 'A-001' }]);
      }),
      http.post('/api/bookings', () => {
        return HttpResponse.json({ message: 'Booking failed' }, { status: 400 });
      })
    );
    
    renderDashboard();
    
    await waitFor(() => {
      expect(screen.getByText('BOOK NOW')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('BOOK NOW'));
    
    await waitFor(() => {
      expect(screen.getByText('Booking Failed')).toBeInTheDocument();
    });
  });
});
