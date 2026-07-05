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
// Components like DemoGuide need to know whether the app is running in
// demo mode so they can surface the demo banner and session-timeout hints.
// (Demo mode does NOT bypass reCAPTCHA — the backend verifies every token,
// so Login/Register always execute it.) The provider sits at the root of
// <App /> and is hydrated
// from /api/config on first render. The default value lets a component
// imported in isolation (e.g. inside a single-component vitest case)
// still get a valid `{ demoMode, sessionTimeout }` object without having
// to mount the full App tree.
const DemoContext = createContext({ demoMode: false, sessionTimeout: 0 });
export const useDemoMode = () => useContext(DemoContext);

// reCAPTCHA v3 site key fallback for local development. The real site
// key is fetched at runtime from /api/config so that production
// deployments can rotate it without a frontend rebuild. This fallback
// is only used if /api/config returns an empty recaptchaSiteKey
// (e.g. during local dev when the env var isn't set).
const FALLBACK_RECAPTCHA_SITE_KEY =
  '6LcvSh0aAAAAALmTghQPJoYv-KjmiyLSvQV8E1zs';

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

  // Read the site key from the runtime config the backend just served.
  // If the backend didn't set one (local dev with no env var), use the
  // public test fallback so reCAPTCHA at least renders on localhost.
  const reCaptchaKey = config.recaptchaSiteKey && config.recaptchaSiteKey.length > 0
    ? config.recaptchaSiteKey
    : FALLBACK_RECAPTCHA_SITE_KEY;

  return (
    <DemoContext.Provider value={config}>
      <AuthProvider>
        <ModernAlertProvider>
          {/* useEnterprise: the registered site key is a reCAPTCHA
              Enterprise (score-based) key, which must be loaded via
              enterprise.js and executed through grecaptcha.enterprise —
              the classic api.js loader silently produces tokens that
              Google's siteverify rejects. The backend still verifies via
              the legacy siteverify endpoint using the key's legacy
              secret, so no server-side change is needed. */}
          <GoogleReCaptchaProvider reCaptchaKey={reCaptchaKey} useEnterprise>
            <AppRoutes />
          </GoogleReCaptchaProvider>
        </ModernAlertProvider>
      </AuthProvider>
    </DemoContext.Provider>
  );
}

export default App;
