import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import CurrentBooking from '../../components/CurrentBooking';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderCurrentBooking = () => {
  localStorage.setItem('token', 'mock-token');
  localStorage.setItem('user', JSON.stringify({ email: 'test@example.com', username: 'testuser', role: 'Customer' }));
  
  return render(
    <MemoryRouter initialEntries={['/current-booking']}>
      <AuthProvider>
        <ModernAlertProvider>
          <Routes>
            <Route path="/current-booking" element={<CurrentBooking />} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('CurrentBooking', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should show loading spinner initially', () => {
    renderCurrentBooking();
    expect(document.querySelector('.spinner-border')).toBeInTheDocument();
  });

  it('should render current booking table headers', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByText('ID')).toBeInTheDocument();
      expect(screen.getByText('User ID')).toBeInTheDocument();
      expect(screen.getByText('Username')).toBeInTheDocument();
      expect(screen.getByText('Contact')).toBeInTheDocument();
      expect(screen.getByText('Credit')).toBeInTheDocument();
      expect(screen.getByText('Car Plate')).toBeInTheDocument();
      expect(screen.getByText('Parking Spot')).toBeInTheDocument();
      expect(screen.getByText('Date')).toBeInTheDocument();
      expect(screen.getByText('Actions')).toBeInTheDocument();
    });
  });

  it('should display welcome message with username', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByText(/Welcome testuser/)).toBeInTheDocument();
    });
  });

  it('should render navigation links', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByText('User page')).toBeInTheDocument();
      expect(screen.getByText('History')).toBeInTheDocument();
      expect(screen.getByText('Profile')).toBeInTheDocument();
      expect(screen.getByText('Current Booking')).toBeInTheDocument();
      expect(screen.getByText('Logout')).toBeInTheDocument();
    });
  });

  it('should show empty state when no active bookings', async () => {
    server.use(
      http.get('/api/bookings/my-active', () => {
        return HttpResponse.json([]);
      })
    );
    
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByText('No active bookings')).toBeInTheDocument();
    });
  });

  it('should display active booking with action buttons', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      // Check for action buttons
      const reportButton = screen.queryByTitle('Report Spot Taken');
      const cancelButton = screen.queryByTitle('Cancel Booking');
      
      expect(reportButton).toBeInTheDocument();
      expect(cancelButton).toBeInTheDocument();
    });
  });

  it('should show confirmation dialog when cancel booking is clicked', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByTitle('Cancel Booking')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Cancel Booking'));
    
    await waitFor(() => {
      expect(screen.getByText('Cancel Booking?')).toBeInTheDocument();
    });
  });

  it('should show confirmation dialog when report spot taken is clicked', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByTitle('Report Spot Taken')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Report Spot Taken'));
    
    await waitFor(() => {
      expect(screen.getByText('Report Spot Taken?')).toBeInTheDocument();
    });
  });

  it('should have Current Booking link marked as active', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      const currentBookingLink = screen.getByText('Current Booking').closest('li');
      expect(currentBookingLink).toHaveClass('active');
    });
  });

  it('should cancel booking successfully', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByTitle('Cancel Booking')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Cancel Booking'));
    
    await waitFor(() => {
      expect(screen.getByText('Cancel Booking?')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Booking Cancelled')).toBeInTheDocument();
    });
  });

  it('should report spot taken successfully', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByTitle('Report Spot Taken')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Report Spot Taken'));
    
    await waitFor(() => {
      expect(screen.getByText('Report Spot Taken?')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Spot Reassigned!')).toBeInTheDocument();
    });
  });

  it('should handle cancel booking error', async () => {
    server.use(
      http.post('/api/bookings/:id/cancel', () => {
        return HttpResponse.json({ message: 'Cancellation failed' }, { status: 400 });
      })
    );
    
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByTitle('Cancel Booking')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Cancel Booking'));
    
    await waitFor(() => {
      expect(screen.getByText('Yes, Confirm')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Cancellation Failed')).toBeInTheDocument();
    });
  });

  it('should handle report spot taken error', async () => {
    server.use(
      http.post('/api/bookings/:id/report-taken', () => {
        return HttpResponse.json('No spots available', { status: 400 });
      })
    );
    
    renderCurrentBooking();
    
    await waitFor(() => {
      expect(screen.getByTitle('Report Spot Taken')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Report Spot Taken'));
    
    await waitFor(() => {
      expect(screen.getByText('Yes, Confirm')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Report Failed')).toBeInTheDocument();
    });
  });

  it('should display booking details', async () => {
    renderCurrentBooking();
    
    await waitFor(() => {
      // Check for booking ID column
      expect(screen.getByText('ID')).toBeInTheDocument();
    });
  });
});
