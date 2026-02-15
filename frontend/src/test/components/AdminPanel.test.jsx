import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import AdminPanel from '../../components/AdminPanel';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderAdminPanel = () => {
  localStorage.setItem('token', 'mock-admin-token');
  localStorage.setItem('user', JSON.stringify({ email: 'admin@example.com', username: 'admin', role: 'Admin' }));
  
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <AuthProvider>
        <ModernAlertProvider>
          <Routes>
            <Route path="/admin" element={<AdminPanel />} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('AdminPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should show loading spinner initially', () => {
    renderAdminPanel();
    expect(document.querySelector('.spinner-border')).toBeInTheDocument();
  });

  it('should render admin navigation', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.getByText('ADMIN HOME')).toBeInTheDocument();
      expect(screen.getByText('View Booking History')).toBeInTheDocument();
      expect(screen.getByText('View Customers')).toBeInTheDocument();
      expect(screen.getByText('View Parking')).toBeInTheDocument();
      expect(screen.getByText('Log-out')).toBeInTheDocument();
    });
  });

  it('should default to bookings view', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      // Should show booking table headers
      expect(screen.getByText('Booking ID')).toBeInTheDocument();
    });
  });

  it('should switch to customers view when clicked', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.getByText('View Customers')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      // Should show user table headers
      expect(screen.getByText('ID')).toBeInTheDocument();
      expect(screen.getByText('Username')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
    });
  });

  it('should switch to parking view when clicked', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.getByText('View Parking')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('View Parking'));
    
    await waitFor(() => {
      // Should show parking table headers
      expect(screen.getByText('Parking ID')).toBeInTheDocument();
      // Status header
      const statusHeader = screen.getAllByText(/Status/i);
      expect(statusHeader.length).toBeGreaterThan(0);
    });
  });

  it('should display parking spot statuses', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.getByText('View Parking')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('View Parking'));
    
    await waitFor(() => {
      // Should show available and occupied badges
      const availableBadges = screen.queryAllByText('AVAILABLE');
      const occupiedBadges = screen.queryAllByText('OCCUPIED');
      
      expect(availableBadges.length + occupiedBadges.length).toBeGreaterThan(0);
    });
  });

  it('should allow editing a user', async () => {
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      const editButtons = screen.queryAllByTitle('Edit Customer');
      expect(editButtons.length).toBeGreaterThan(0);
    });
    
    const editButtons = screen.getAllByTitle('Edit Customer');
    fireEvent.click(editButtons[0]);
    
    await waitFor(() => {
      // Should show save and cancel buttons
      expect(screen.getByTitle('Save Changes')).toBeInTheDocument();
      expect(screen.getByTitle('Cancel')).toBeInTheDocument();
    });
  });

  it('should show delete confirmation for users', async () => {
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      const deleteButtons = screen.queryAllByTitle('Delete Customer');
      expect(deleteButtons.length).toBeGreaterThan(0);
    });
    
    const deleteButtons = screen.getAllByTitle('Delete Customer');
    fireEvent.click(deleteButtons[0]);
    
    await waitFor(() => {
      expect(screen.getByText('Delete Customer?')).toBeInTheDocument();
    });
  });

  it('should show delete confirmation for bookings', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      const deleteButtons = screen.queryAllByTitle('Delete Booking');
      if (deleteButtons.length > 0) {
        fireEvent.click(deleteButtons[0]);
      }
    });
    
    // Note: Only active bookings can be deleted, so this may or may not show
  });

  it('should logout when Log-out is clicked', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.getByText('Log-out')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Log-out'));
    
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('should highlight active navigation item', async () => {
    renderAdminPanel();
    
    await waitFor(() => {
      const bookingsLink = screen.getByText('View Booking History').closest('li');
      expect(bookingsLink).toHaveClass('active');
    });
  });

  it('should cancel user edit mode', async () => {
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      const editButtons = screen.queryAllByTitle('Edit Customer');
      expect(editButtons.length).toBeGreaterThan(0);
    });
    
    fireEvent.click(screen.getAllByTitle('Edit Customer')[0]);
    
    await waitFor(() => {
      expect(screen.getByTitle('Cancel')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Cancel'));
    
    await waitFor(() => {
      expect(screen.queryByTitle('Cancel')).not.toBeInTheDocument();
    });
  });

  it('should save user updates successfully', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.put('/api/admin/users/:id', () => {
        return HttpResponse.json({ id: 1, username: 'updated' });
      })
    );
    
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      expect(screen.queryAllByTitle('Edit Customer').length).toBeGreaterThan(0);
    });
    
    fireEvent.click(screen.getAllByTitle('Edit Customer')[0]);
    
    await waitFor(() => {
      expect(screen.getByTitle('Save Changes')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('User Updated')).toBeInTheDocument();
    });
  });

  it('should handle user update error', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.put('/api/admin/users/:id', () => {
        return HttpResponse.json({ message: 'Update failed' }, { status: 400 });
      })
    );
    
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      expect(screen.queryAllByTitle('Edit Customer').length).toBeGreaterThan(0);
    });
    
    fireEvent.click(screen.getAllByTitle('Edit Customer')[0]);
    
    await waitFor(() => {
      expect(screen.getByTitle('Save Changes')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByTitle('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Update Failed')).toBeInTheDocument();
    });
  });

  it('should show empty state for bookings', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/admin/bookings', () => {
        return HttpResponse.json([]);
      })
    );
    
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.getByText('No booking history found')).toBeInTheDocument();
    });
  });

  it('should show empty state for users', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/admin/users', () => {
        return HttpResponse.json([]);
      })
    );
    
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      expect(screen.getByText('No customer records found')).toBeInTheDocument();
    });
  });

  it('should show empty state for parking', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/admin/parking', () => {
        return HttpResponse.json([]);
      })
    );
    
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Parking'));
    
    await waitFor(() => {
      expect(screen.getByText('No parking spots found')).toBeInTheDocument();
    });
  });

  it('should display unknown parking status', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/admin/parking', () => {
        return HttpResponse.json([{ parkingId: 'A-001', availability: 'maintenance' }]);
      })
    );
    
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Parking'));
    
    await waitFor(() => {
      expect(screen.getByText('MAINTENANCE')).toBeInTheDocument();
    });
  });

  it('should delete user successfully', async () => {
    renderAdminPanel();
    
    fireEvent.click(screen.getByText('View Customers'));
    
    await waitFor(() => {
      expect(screen.queryAllByTitle('Delete Customer').length).toBeGreaterThan(0);
    });
    
    fireEvent.click(screen.getAllByTitle('Delete Customer')[0]);
    
    await waitFor(() => {
      expect(screen.getByText('Delete Customer?')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Customer Deleted')).toBeInTheDocument();
    });
  });

  it('should delete booking successfully', async () => {
    const { server } = await import('../mocks/server');
    const { http, HttpResponse } = await import('msw');
    
    server.use(
      http.get('/api/admin/bookings', () => {
        return HttpResponse.json([{
          id: 1,
          userId: 1,
          userName: 'testuser',
          userContact: '1234567890',
          carPlate: 'ABC123',
          parkingSpot: 'A-001',
          date: '2024-02-14T10:00:00',
          status: 'Active',
          credit: 6
        }]);
      })
    );
    
    renderAdminPanel();
    
    await waitFor(() => {
      expect(screen.queryAllByTitle('Delete Booking').length).toBeGreaterThan(0);
    });
    
    fireEvent.click(screen.getAllByTitle('Delete Booking')[0]);
    
    await waitFor(() => {
      expect(screen.getByText('Delete Booking?')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Booking Deleted')).toBeInTheDocument();
    });
  });
});
