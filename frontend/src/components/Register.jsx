import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useGoogleReCaptcha } from 'react-google-recaptcha-v3';
import { useModernAlert } from './ModernAlert';
import { useDemoMode } from '../App';
import Navbar from './Navbar';

export default function Register() {
  const [formData, setFormData] = useState({ 
    username: '', 
    email: '', 
    password: '', 
    confirmPassword: '',
    mobile: '',
    address: '',
    carPlateNo: ''
  });
  const [error, setError] = useState('');
  const { executeRecaptcha } = useGoogleReCaptcha?.() || {};
  const { demoMode } = useDemoMode();
  const navigate = useNavigate();
  const modernAlert = useModernAlert();

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');

    if (formData.password !== formData.confirmPassword) {
        setError("Passwords do not match!");
        return;
    }

    try {
      let recaptchaToken = 'demo-token';
      
      if (!demoMode) {
        // reCAPTCHA v3: Execute in background with 'register' action
        if (!executeRecaptcha) {
          setError('reCAPTCHA not ready. Please try again.');
          return;
        }
        recaptchaToken = await executeRecaptcha('register');
      }
      
      // Strip confirmPassword before posting; the rename to _confirmPassword
      // satisfies ESLint's "unused var" rule (allowed prefix is `_`) while
      // keeping the field out of the API payload.
      // eslint-disable-next-line no-unused-vars
      const { confirmPassword: _confirmPassword, ...registrationData } = formData;
      await axios.post('/api/users/register', {
        ...registrationData,
        recaptchaToken
      });
      modernAlert.success('Registration Successful!', 'Your account has been created.<br><br>Please login to start making reservations.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    }
  };

  const handleChange = (e) => {
      setFormData({...formData, [e.target.name]: e.target.value});
  };

  return (
    <>
      <Navbar />
      <div className="main">
        <div className="main-agilerow">
          <div className="signup-wthreetop">
            <h2>Register for Parking</h2>
            <p>Create your account to start reserving parking spaces</p>
          </div>
          <div className="contact-wthree">
            <form onSubmit={handleRegister}>
              {error && (
                <div className="alert alert-danger" style={{marginBottom: '15px'}}>
                  {error}
                </div>
              )}
              
              <div className="form-w3step1">
                <input 
                  type="text" 
                  name="username"
                  placeholder="👤 Full Name" 
                  onChange={handleChange} 
                  required
                />
                <input 
                  type="email" 
                  name="email"
                  placeholder="📧 Email Address" 
                  onChange={handleChange} 
                  required
                />
                <input 
                  type="password" 
                  name="password"
                  placeholder="🔒 Password" 
                  onChange={handleChange} 
                  required
                  pattern="(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?&quot;:{}|&lt;&gt;]).{8,}"
                  title="Must contain at least 8 characters with uppercase, lowercase, number, and special character (!@#$%^&*)"
                />
                <input 
                  type="password" 
                  name="confirmPassword"
                  placeholder="🔒 Confirm Password" 
                  onChange={handleChange} 
                  required
                />
                <input 
                  type="text" 
                  name="mobile"
                  placeholder="📱 Mobile Number" 
                  onChange={handleChange} 
                  required
                />
                <input 
                  type="text" 
                  name="address"
                  placeholder="📍 Your Address" 
                  onChange={handleChange} 
                  required
                />
                <input 
                  type="text" 
                  name="carPlateNo"
                  placeholder="🚗 Car Plate Number" 
                  onChange={handleChange} 
                  required
                  className="agileits-btm"
                />
              </div>

              <button type="submit" className="btn-register">CREATE ACCOUNT</button>
            </form>
          </div>
        </div>
      </div>
    </>
  );
}
