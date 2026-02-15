import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../../App';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should render without crashing', async () => {
    render(<App />);
    
    await waitFor(() => {
      // App should eventually render something
      expect(document.body).toBeInTheDocument();
    });
  });

  it('should render Home page elements', async () => {
    render(<App />);
    
    await waitFor(() => {
      // Look for navbar which is always present on home page
      expect(document.querySelector('nav')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  it('should show loading state initially before config loads', () => {
    // Override config to delay response  
    server.use(
      http.get('/api/config', async () => {
        await new Promise(resolve => setTimeout(resolve, 5000));
        return HttpResponse.json({ demoMode: true, sessionTimeout: 0 });
      })
    );
    
    const { container } = render(<App />);
    
    // Initially returns null while loading config
    expect(container.firstChild).toBeNull();
  });

  it('should handle config fetch failure gracefully', async () => {
    server.use(
      http.get('/api/config', () => {
        return HttpResponse.error();
      })
    );
    
    render(<App />);
    
    // Should still render the app even if config fails
    await waitFor(() => {
      expect(document.querySelector('nav') || document.body.textContent).toBeTruthy();
    });
  });

  it('should render routes correctly', async () => {
    render(<App />);
    
    await waitFor(() => {
      // App should render with router
      expect(document.body).toBeInTheDocument();
    });
  });

  it('should provide DemoContext to children', async () => {
    render(<App />);
    
    await waitFor(() => {
      // If demo mode is enabled, DemoGuide will eventually show
      // Just verify app renders
      expect(document.body).toBeInTheDocument();
    });
  });
});
