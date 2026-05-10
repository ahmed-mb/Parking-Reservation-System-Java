import { useState, useEffect, createContext, useContext } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import axios from 'axios';
import { GoogleReCaptchaProvider } from 'react-google-recaptcha-v3';
import { AuthProvider } from './context/AuthContext';
import { ModernAlertProvider } from './components/ModernAlert';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './components/Home';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import Register from './components/Register';
import BookingHistory from './components/BookingHistory';
import UserProfile from './components/UserProfile';
import CurrentBooking from './components/CurrentBooking';
import AdminPanel from './components/AdminPanel';
import NotFound from './components/NotFound';
import DemoGuide from './components/DemoGuide';

// ---------------------------------------------------------------------------
// Demo-mode context
// ---------------------------------------------------------------------------
// Several components (Login, Register, DemoGuide) need to know whether the
// app is running in demo mode so they can bypass reCAPTCHA and surface the
// demo banner. The provider sits at the root of <App /> and is hydrated
// from /api/config on first render. The default value lets a component
// imported in isolation (e.g. inside a single-component vitest case)
// still get a valid `{ demoMode, sessionTimeout }` object without having
// to mount the full App tree.
const DemoContext = createContext({ demoMode: false, sessionTimeout: 0 });
export const useDemoMode = () => useContext(DemoContext);

// reCAPTCHA v3 site key. Pulled from a Vite env var at build time so the
// production deployment can supply its own key without code changes; falls
// back to the project's documented public test key for local dev.
const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY
  || '6LcvSh0aAAAAALmTghQPJoYv-KjmiyLSvQV8E1zs';

function AppRoutes() {
  return (
    <Router>
      <div className="container">
        <DemoGuide />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/history"
            element={
              <ProtectedRoute>
                <BookingHistory />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <UserProfile />
              </ProtectedRoute>
            }
          />
          <Route
            path="/current-booking"
            element={
              <ProtectedRoute>
                <CurrentBooking />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute requireAdmin={true}>
                <AdminPanel />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
    </Router>
  );
}

function App() {
  // Config is fetched from the backend before any UI renders so that
  // children (which read it via useDemoMode) always see a real value
  // rather than the static default. Returning null until the response
  // arrives is what the App.test "loading state" test asserts on.
  const [config, setConfig] = useState(null);

  useEffect(() => {
    axios.get('/api/config')
      .then((res) => setConfig(res.data))
      .catch(() => {
        // If /api/config fails (network down, backend cold-starting),
        // fall back to non-demo mode rather than blocking the UI forever.
        setConfig({ demoMode: false, sessionTimeout: 0 });
      });
  }, []);

  if (config === null) return null;

  return (
    <DemoContext.Provider value={config}>
      <AuthProvider>
        <ModernAlertProvider>
          <GoogleReCaptchaProvider reCaptchaKey={RECAPTCHA_SITE_KEY}>
            <AppRoutes />
          </GoogleReCaptchaProvider>
        </ModernAlertProvider>
      </AuthProvider>
    </DemoContext.Provider>
  );
}

export default App;
