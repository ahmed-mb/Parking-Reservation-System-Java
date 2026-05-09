import { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useGoogleReCaptcha } from 'react-google-recaptcha-v3';
import { useDemoMode } from '../App';
import Navbar from './Navbar';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { executeRecaptcha } = useGoogleReCaptcha?.() || {};
  const { demoMode } = useDemoMode();
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      let recaptchaToken = 'demo-token';
      
      if (!demoMode) {
        // reCAPTCHA v3: Execute in background with 'login' action
        if (!executeRecaptcha) {
          setError('reCAPTCHA not ready. Please try again.');
          setLoading(false);
          return;
        }
        recaptchaToken = await executeRecaptcha('login');
      }

      const res = await axios.post('/api/users/login', { 
        email, 
        password,
        recaptchaToken 
      });
      const { token, email: userEmail, username, role } = res.data;
      
      login({ email: userEmail, username, role }, token);
      
      // Redirect based on role
      if (role === 'Admin') {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Navbar />
      <div className="main">
        <div className="main-agilerow">
          <div className="signup-wthreetop">
            <h2>Welcome Back</h2>
            <p>Sign in to access your parking reservations</p>
          </div>
          <div className="contact-wthree">
            <form onSubmit={handleLogin}>
              {error && (
                <div className="alert alert-danger" style={{marginBottom: '15px'}}>
                  {error}
                </div>
              )}
              
              <div className="form-w3step1">
                <input 
                  type="email" 
                  placeholder="📧 Email Address" 
                  value={email} 
                  onChange={e => setEmail(e.target.value)} 
                  required 
                  disabled={loading}
                />
                <input 
                  type="password" 
                  placeholder="🔒 Password" 
                  value={password} 
                  onChange={e => setPassword(e.target.value)} 
                  required 
                  disabled={loading}
                />
              </div>

              <button type="submit" disabled={loading}>
                {loading ? 'LOGGING IN...' : 'LOGIN'}
              </button>

              <div style={{marginTop: '20px', textAlign: 'center'}}>
                <Link to="/register" style={{color: '#667eea', textDecoration: 'none'}}>
                  Don&apos;t have an account? Register here
                </Link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
}
