import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import { ModernAlertProvider } from '../../components/ModernAlert';
import Home from '../../components/Home';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';

const renderHome = () => {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <ModernAlertProvider>
          <Home />
        </ModernAlertProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('Home', () => {
  it('should render welcome message', async () => {
    localStorage.getItem.mockReturnValue(null);
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByText(/Welcome to Parking Reservation System/i)).toBeInTheDocument();
    });
  });

  it('should render check availability button', async () => {
    localStorage.getItem.mockReturnValue(null);
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Check Availability/i })).toBeInTheDocument();
    });
  });

  it('should render login button', async () => {
    localStorage.getItem.mockReturnValue(null);
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Login/i })).toBeInTheDocument();
    });
  });

  it('should show availability modal with spots count when clicked', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Check Availability/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Check Availability/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Parking Availability/i)).toBeInTheDocument();
      expect(screen.getByText(/parking spaces/i)).toBeInTheDocument();
    });
  });

  it('should close modal when close button is clicked', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Check Availability/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Check Availability/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Parking Availability/i)).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Close/i }));
    
    await waitFor(() => {
      expect(screen.queryByText(/Parking Availability/i)).not.toBeInTheDocument();
    });
  });

  it('should close modal when clicking backdrop', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Check Availability/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Check Availability/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Parking Availability/i)).toBeInTheDocument();
    });

    // Click the backdrop
    const backdrop = document.querySelector('.modern-alert-backdrop');
    fireEvent.click(backdrop);
    
    await waitFor(() => {
      expect(screen.queryByText(/Parking Availability/i)).not.toBeInTheDocument();
    });
  });

  it('should not close modal when clicking modal content', async () => {
    localStorage.getItem.mockReturnValue(null);
    const user = userEvent.setup();
    renderHome();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Check Availability/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Check Availability/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Parking Availability/i)).toBeInTheDocument();
    });

    // Click the modal content (not backdrop)
    const modal = document.querySelector('.modern-alert-modal');
    fireEvent.click(modal);
    
    // Modal should still be visible
    expect(screen.getByText(/Parking Availability/i)).toBeInTheDocument();
  });

  it('should render navbar component', () => {
    localStorage.getItem.mockReturnValue(null);
    renderHome();
    
    expect(document.querySelector('nav')).toBeInTheDocument();
  });

  it('should have correct page structure', () => {
    localStorage.getItem.mockReturnValue(null);
    renderHome();
    
    // Check that main content areas exist
    expect(document.querySelector('.main')).toBeInTheDocument();
  });
});
