import { useState, useEffect, createContext, useContext } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
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

// Demo mode context
const DemoContext = createContext({ demoMode: false, sessionTimeout: 0 });
export const useDemoMode = () => useContext(DemoContext);

const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY || '6LcvSh0aAAAAALmTghQPJoYv-KjmiyLSvQV8E1zs';

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
  const [demoConfig, setDemoConfig] = useState({ demoMode: false, sessionTimeout: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Fetch demo config from backend
    fetch('/api/config')
      .then(res => res.json())
      .then(data => {
        setDemoConfig(data);
        setLoading(false);
      })
      .catch(() => {
        // If config endpoint fails, assume not demo mode
        setLoading(false);
      });
  }, []);

  if (loading) {
    return null; // Brief flash while checking config
  }

  const content = (
    <DemoContext.Provider value={demoConfig}>
      <AuthProvider>
        <ModernAlertProvider>
          <AppRoutes />
        </ModernAlertProvider>
      </AuthProvider>
    </DemoContext.Provider>
  );

  // In demo mode, skip reCAPTCHA provider entirely
  if (demoConfig.demoMode) {
    return content;
  }

  return (
    <GoogleReCaptchaProvider reCaptchaKey={RECAPTCHA_SITE_KEY}>
      {content}
    </GoogleReCaptchaProvider>
  );
}

export default App;
