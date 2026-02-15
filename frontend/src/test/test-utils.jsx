import React from 'react';
import { render } from '@testing-library/react';
import { BrowserRouter, MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { ModernAlertProvider } from '../components/ModernAlert';
import { vi } from 'vitest';

// Mock demo mode context
const DemoContext = React.createContext({ demoMode: true, sessionTimeout: 0 });
export const MockDemoProvider = ({ children, demoMode = true }) => (
  <DemoContext.Provider value={{ demoMode, sessionTimeout: 0 }}>
    {children}
  </DemoContext.Provider>
);

// Custom render function with all providers
export function renderWithProviders(
  ui,
  {
    route = '/',
    demoMode = true,
    ...renderOptions
  } = {}
) {
  window.history.pushState({}, 'Test page', route);

  function Wrapper({ children }) {
    return (
      <MockDemoProvider demoMode={demoMode}>
        <MemoryRouter initialEntries={[route]}>
          <AuthProvider>
            <ModernAlertProvider>
              {children}
            </ModernAlertProvider>
          </AuthProvider>
        </MemoryRouter>
      </MockDemoProvider>
    );
  }

  return {
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
  };
}

// Helper to create a mock authenticated user
export function mockAuthenticatedUser(user = {
  email: 'test@example.com',
  username: 'testuser',
  role: 'Customer'
}) {
  localStorage.getItem.mockImplementation((key) => {
    if (key === 'user') return JSON.stringify(user);
    if (key === 'token') return 'mock-jwt-token';
    return null;
  });
}

// Helper to clear auth
export function clearMockAuth() {
  localStorage.getItem.mockReturnValue(null);
}

// Re-export everything
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';
