import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ModernAlertProvider, useModernAlert } from '../../components/ModernAlert';

// Test component that uses the alert
function TestComponent() {
  const alert = useModernAlert();
  
  return (
    <div>
      <button onClick={() => alert.success('Success!', 'Operation completed')}>Show Success</button>
      <button onClick={() => alert.error('Error!', 'Something went wrong')}>Show Error</button>
      <button onClick={() => alert.warning('Warning!', 'Be careful')}>Show Warning</button>
      <button onClick={() => alert.info('Info', 'Just FYI')}>Show Info</button>
      <button onClick={() => alert.confirmCancelBooking(() => {}, () => {})}>Show Cancel Confirm</button>
      <button onClick={() => alert.confirmDeleteBooking(() => {}, () => {})}>Show Delete Confirm</button>
      <button onClick={() => alert.confirmReportSpotTaken(() => {}, () => {})}>Show Report Confirm</button>
      <button onClick={() => alert.confirmDeleteCustomer(() => {}, () => {})}>Show Delete Customer</button>
    </div>
  );
}

const renderWithProvider = () => {
  return render(
    <ModernAlertProvider>
      <TestComponent />
    </ModernAlertProvider>
  );
};

describe('ModernAlert', () => {
  it('should show success alert', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Success'));
    
    await waitFor(() => {
      expect(screen.getByText('Success!')).toBeInTheDocument();
      expect(screen.getByText('Operation completed')).toBeInTheDocument();
    });
  });

  it('should show error alert', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Error'));
    
    await waitFor(() => {
      expect(screen.getByText('Error!')).toBeInTheDocument();
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });
  });

  it('should show warning alert', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Warning'));
    
    await waitFor(() => {
      expect(screen.getByText('Warning!')).toBeInTheDocument();
      expect(screen.getByText('Be careful')).toBeInTheDocument();
    });
  });

  it('should show info alert', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Info'));
    
    await waitFor(() => {
      expect(screen.getByText('Info')).toBeInTheDocument();
      expect(screen.getByText('Just FYI')).toBeInTheDocument();
    });
  });

  it('should close alert when close button clicked', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Success'));
    
    await waitFor(() => {
      expect(screen.getByText('Success!')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Close'));
    
    await waitFor(() => {
      expect(screen.queryByText('Success!')).not.toBeInTheDocument();
    });
  });

  it('should close alert when clicking backdrop', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Success'));
    
    await waitFor(() => {
      expect(screen.getByText('Success!')).toBeInTheDocument();
    });

    const backdrop = document.querySelector('.modern-alert-backdrop');
    fireEvent.click(backdrop);
    
    await waitFor(() => {
      expect(screen.queryByText('Success!')).not.toBeInTheDocument();
    });
  });

  it('should show confirm dialog with Yes/No buttons', async () => {
    const user = userEvent.setup();
    renderWithProvider();
    
    await user.click(screen.getByText('Show Cancel Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Cancel Booking?')).toBeInTheDocument();
      expect(screen.getByText('No, Keep It')).toBeInTheDocument();
      expect(screen.getByText('Yes, Confirm')).toBeInTheDocument();
    });
  });

  it('should call onConfirm when Yes clicked', async () => {
    const onConfirmMock = vi.fn();
    
    function TestConfirmComponent() {
      const alert = useModernAlert();
      return (
        <button onClick={() => alert.confirmCancelBooking(onConfirmMock, () => {})}>
          Confirm
        </button>
      );
    }
    
    render(
      <ModernAlertProvider>
        <TestConfirmComponent />
      </ModernAlertProvider>
    );
    
    fireEvent.click(screen.getByText('Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Yes, Confirm')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Yes, Confirm'));
    
    // Wait for the callback to be called after animation
    await waitFor(() => {
      expect(onConfirmMock).toHaveBeenCalled();
    }, { timeout: 1000 });
  });

  it('should call onCancel when No clicked', async () => {
    const onCancelMock = vi.fn();
    
    function TestCancelComponent() {
      const alert = useModernAlert();
      return (
        <button onClick={() => alert.confirmCancelBooking(() => {}, onCancelMock)}>
          Confirm
        </button>
      );
    }
    
    render(
      <ModernAlertProvider>
        <TestCancelComponent />
      </ModernAlertProvider>
    );
    
    fireEvent.click(screen.getByText('Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('No, Keep It')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('No, Keep It'));
    
    // Wait for the callback to be called after animation
    await waitFor(() => {
      expect(onCancelMock).toHaveBeenCalled();
    }, { timeout: 1000 });
  });

  it('should not close confirm dialog when clicking backdrop', async () => {
    renderWithProvider();
    
    fireEvent.click(screen.getByText('Show Cancel Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Cancel Booking?')).toBeInTheDocument();
    });

    const backdrop = document.querySelector('.modern-alert-backdrop');
    fireEvent.click(backdrop);
    
    // Confirm dialog should still be visible
    expect(screen.getByText('Cancel Booking?')).toBeInTheDocument();
  });

  it('should show delete booking confirm', async () => {
    renderWithProvider();
    
    fireEvent.click(screen.getByText('Show Delete Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Delete Booking?')).toBeInTheDocument();
    });
  });

  it('should show report spot taken confirm', async () => {
    renderWithProvider();
    
    fireEvent.click(screen.getByText('Show Report Confirm'));
    
    await waitFor(() => {
      expect(screen.getByText('Report Spot Taken?')).toBeInTheDocument();
    });
  });

  it('should show delete customer confirm', async () => {
    renderWithProvider();
    
    fireEvent.click(screen.getByText('Show Delete Customer'));
    
    await waitFor(() => {
      expect(screen.getByText('Delete Customer?')).toBeInTheDocument();
    });
  });
});
