import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NotFound from '../../components/NotFound';

const renderNotFound = () => {
  return render(
    <MemoryRouter>
      <NotFound />
    </MemoryRouter>
  );
};

describe('NotFound', () => {
  it('should render 404 digits', () => {
    renderNotFound();
    
    // The component uses 4, car emoji, 4
    const digits = screen.getAllByText('4');
    expect(digits.length).toBeGreaterThanOrEqual(1);
  });

  it('should render error title', () => {
    renderNotFound();
    
    expect(screen.getByText(/Parking Spot Not Found/i)).toBeInTheDocument();
  });

  it('should have links to home and login', () => {
    renderNotFound();
    
    expect(screen.getByText('Back to Home')).toBeInTheDocument();
    expect(screen.getAllByText('Login').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Register').length).toBeGreaterThan(0);
  });

  it('should have a go back link', () => {
    const historyBackSpy = vi.spyOn(window.history, 'back').mockImplementation(() => {});
    renderNotFound();
    
    const goBackLink = screen.getByText('Go Back');
    fireEvent.click(goBackLink);
    
    expect(historyBackSpy).toHaveBeenCalled();
    historyBackSpy.mockRestore();
  });

  it('should display error code text', () => {
    renderNotFound();
    
    expect(screen.getByText('Error Code: 404')).toBeInTheDocument();
  });
});
