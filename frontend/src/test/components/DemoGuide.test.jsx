import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import DemoGuide from '../../components/DemoGuide';

// Mock useDemoMode with different states
let mockDemoMode = true;

vi.mock('../../App', () => ({
  useDemoMode: () => ({ demoMode: mockDemoMode, sessionTimeout: 0 })
}));

describe('DemoGuide', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    mockDemoMode = true;
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should not render when demo mode is disabled', () => {
    mockDemoMode = false;
    const { container } = render(<DemoGuide />);
    expect(container.firstChild).toBeNull();
  });

  it('should show guide after delay in demo mode', async () => {
    render(<DemoGuide />);
    
    // Initially not visible (waiting for delay)
    expect(screen.queryByText('Welcome to the Parking Reservation System')).not.toBeInTheDocument();
    
    // Advance timers
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    expect(screen.getByText('Welcome to the Parking Reservation System')).toBeInTheDocument();
  });

  it('should display step indicator', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    expect(screen.getByText('Step 1 of 5')).toBeInTheDocument();
  });

  it('should navigate to next step when Next is clicked', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    expect(screen.getByText('Next')).toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Next'));
    
    expect(screen.getByText('Step 2 of 5')).toBeInTheDocument();
    expect(screen.getByText('Try the Admin Panel')).toBeInTheDocument();
  });

  it('should show Back button after first step', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    // On first step, no Back button
    expect(screen.queryByText('Back')).not.toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Next'));
    
    expect(screen.getByText('Back')).toBeInTheDocument();
  });

  it('should navigate back when Back is clicked', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    fireEvent.click(screen.getByText('Next'));
    expect(screen.getByText('Step 2 of 5')).toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Back'));
    expect(screen.getByText('Step 1 of 5')).toBeInTheDocument();
  });

  it('should close guide when Skip is clicked', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    expect(screen.getByText('Skip')).toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Skip'));
    
    // Guide should be minimized, show floating button
    expect(screen.queryByText('Welcome to the Parking Reservation System')).not.toBeInTheDocument();
  });

  it('should show floating help button when minimized', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    fireEvent.click(screen.getByText('Skip'));
    
    expect(screen.getByTitle('Demo Guide')).toBeInTheDocument();
  });

  it('should reopen guide when floating button is clicked', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    fireEvent.click(screen.getByText('Skip'));
    expect(screen.getByTitle('Demo Guide')).toBeInTheDocument();
    
    fireEvent.click(screen.getByTitle('Demo Guide'));
    
    expect(screen.getByText('Welcome to the Parking Reservation System')).toBeInTheDocument();
  });

  it('should show Get Started on last step', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    // Navigate to last step (5 steps total)
    for (let i = 0; i < 4; i++) {
      fireEvent.click(screen.getByText('Next'));
    }
    
    expect(screen.getByText('Get Started')).toBeInTheDocument();
    expect(screen.getByText('Step 5 of 5')).toBeInTheDocument();
  });

  it('should show minimized button if previously dismissed', async () => {
    sessionStorage.setItem('demoGuideDismissed', 'true');
    
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    // Should show floating button directly
    expect(screen.getByTitle('Demo Guide')).toBeInTheDocument();
    expect(screen.queryByText('Welcome to the Parking Reservation System')).not.toBeInTheDocument();
  });

  it('should close on Get Started click', async () => {
    render(<DemoGuide />);
    
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    
    // Navigate to last step
    for (let i = 0; i < 4; i++) {
      fireEvent.click(screen.getByText('Next'));
    }
    
    fireEvent.click(screen.getByText('Get Started'));
    
    // Guide should close
    expect(screen.queryByText('Tech Stack')).not.toBeInTheDocument();
  });
});
