import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Navbar from '../../components/Navbar';

const renderNavbar = () => {
  return render(
    <MemoryRouter>
      <Navbar />
    </MemoryRouter>
  );
};

describe('Navbar', () => {
  it('should render brand logo', () => {
    renderNavbar();
    expect(screen.getByText(/PARKING SYSTEM/i)).toBeInTheDocument();
  });

  it('should show home link', () => {
    renderNavbar();
    expect(screen.getByText('Home')).toBeInTheDocument();
  });

  it('should show login link', () => {
    renderNavbar();
    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  it('should show register link', () => {
    renderNavbar();
    expect(screen.getByText('Register')).toBeInTheDocument();
  });

  it('should have correct link destinations', () => {
    renderNavbar();
    
    expect(screen.getByText('Home').closest('a')).toHaveAttribute('href', '/');
    expect(screen.getByText('Login').closest('a')).toHaveAttribute('href', '/login');
    expect(screen.getByText('Register').closest('a')).toHaveAttribute('href', '/register');
  });
});
