import { useState, useEffect } from 'react';
import { useDemoMode } from '../App';

const STEPS = [
  {
    title: 'Welcome to the Parking Reservation System',
    content: `This is a <strong>full-stack demo</strong> of a Parking Reservation System, 
      migrated from ASP.NET Web Forms to <strong>Java Spring Boot</strong> (backend) + <strong>React</strong> (frontend).
      <br/><br/>
      Feel free to explore all features!`,
    icon: 'P'
  },
  {
    title: 'Try the Admin Panel',
    content: `Log in as admin to manage users, bookings, and parking spots:
      <br/><br/>
      <div style="background:#1a1a2e;padding:12px 16px;border-radius:8px;font-family:monospace;font-size:14px">
        <span style="color:#888">Email:</span> <span style="color:#4fc3f7">admin@parking.com</span><br/>
        <span style="color:#888">Password:</span> <span style="color:#4fc3f7">Admin@123</span>
      </div>
      <br/>
      From the admin panel you can view all customers, manage bookings, and monitor parking availability.`,
    icon: 'A'
  },
  {
    title: 'Register a New User',
    content: `Create a customer account to experience the user flow:
      <br/><br/>
      <strong>1.</strong> Go to <strong>Register</strong> and fill in your details<br/>
      <strong>2.</strong> Password must have 8+ chars with uppercase, lowercase, number, and special character<br/>
      <strong>3.</strong> After registering, log in to access the customer dashboard
      <br/><br/>
      <em style="color:#aaa">Tip: Try password like <code style="background:#1a1a2e;padding:2px 6px;border-radius:4px">Test@1234</code></em>`,
    icon: 'R'
  },
  {
    title: 'Book a Parking Spot',
    content: `As a logged-in customer, you can:
      <br/><br/>
      <strong>Dashboard</strong> - Select an available spot and book it<br/>
      <strong>Current Booking</strong> - View your active booking, report a taken spot, or cancel<br/>
      <strong>Booking History</strong> - See all past bookings<br/>
      <strong>Profile</strong> - Edit your profile with inline table editing
      <br/><br/>
      The system includes credit management, spot reassignment, and more.`,
    icon: 'B'
  },
  {
    title: 'Tech Stack',
    content: `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:14px">
        <div><strong>Backend:</strong> Java 17, Spring Boot 3.2</div>
        <div><strong>Frontend:</strong> React 18, Vite 5</div>
        <div><strong>Auth:</strong> JWT + BCrypt</div>
        <div><strong>Database:</strong> H2 (demo) / SQL Server</div>
        <div><strong>Security:</strong> IDOR protection, RBAC</div>
        <div><strong>Infra:</strong> Docker, multi-stage build</div>
      </div>
      <br/>
      The full source code showcases migration from ASP.NET, security hardening, 
      and modern Java/React best practices.`,
    icon: 'T'
  }
];

export default function DemoGuide() {
  // `sessionTimeout` is also exposed by useDemoMode but not currently rendered;
  // it is intentionally not destructured here to keep ESLint clean.
  const { demoMode } = useDemoMode();
  const [visible, setVisible] = useState(false);
  const [step, setStep] = useState(0);
  const [minimized, setMinimized] = useState(false);

  useEffect(() => {
    if (demoMode) {
      // Show guide after a short delay on first load
      const dismissed = sessionStorage.getItem('demoGuideDismissed');
      if (!dismissed) {
        const timer = setTimeout(() => setVisible(true), 500);
        return () => clearTimeout(timer);
      } else {
        setMinimized(true);
      }
    }
  }, [demoMode]);

  if (!demoMode) return null;

  const handleClose = () => {
    setVisible(false);
    setMinimized(true);
    sessionStorage.setItem('demoGuideDismissed', 'true');
  };

  const handleReopen = () => {
    setStep(0);
    setVisible(true);
    setMinimized(false);
  };

  const currentStep = STEPS[step];
  const isLast = step === STEPS.length - 1;
  const isFirst = step === 0;

  // Floating help button when minimized
  if (minimized && !visible) {
    return (
      <button
        onClick={handleReopen}
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          background: 'linear-gradient(135deg, #667eea, #764ba2)',
          color: '#fff',
          border: 'none',
          fontSize: '22px',
          fontWeight: 'bold',
          cursor: 'pointer',
          boxShadow: '0 4px 20px rgba(102,126,234,0.5)',
          zIndex: 9999,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transition: 'transform 0.2s',
        }}
        onMouseEnter={e => e.target.style.transform = 'scale(1.1)'}
        onMouseLeave={e => e.target.style.transform = 'scale(1)'}
        title="Demo Guide"
      >
        ?
      </button>
    );
  }

  if (!visible) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100%',
      height: '100%',
      background: 'rgba(0,0,0,0.7)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 10000,
      animation: 'fadeIn 0.3s ease',
    }}>
      <style>{`
        @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
      `}</style>
      <div style={{
        background: 'linear-gradient(145deg, #0f0f23, #1a1a3e)',
        borderRadius: '16px',
        padding: '0',
        maxWidth: '520px',
        width: '90%',
        boxShadow: '0 20px 60px rgba(0,0,0,0.5), 0 0 0 1px rgba(102,126,234,0.2)',
        animation: 'slideUp 0.4s ease',
        overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{
          background: 'linear-gradient(135deg, #667eea, #764ba2)',
          padding: '24px 28px',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
        }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '12px',
            background: 'rgba(255,255,255,0.2)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '22px',
            fontWeight: 'bold',
            color: '#fff',
            flexShrink: 0,
          }}>
            {currentStep.icon}
          </div>
          <div>
            <h3 style={{
              margin: 0,
              color: '#fff',
              fontSize: '18px',
              fontWeight: 600,
            }}>
              {currentStep.title}
            </h3>
            <div style={{
              color: 'rgba(255,255,255,0.7)',
              fontSize: '13px',
              marginTop: '4px',
            }}>
              Step {step + 1} of {STEPS.length}
            </div>
          </div>
        </div>

        {/* Body */}
        <div style={{
          padding: '24px 28px',
          color: '#ccc',
          fontSize: '15px',
          lineHeight: '1.7',
        }}
          dangerouslySetInnerHTML={{ __html: currentStep.content }}
        />

        {/* Progress dots */}
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          gap: '8px',
          padding: '0 28px',
        }}>
          {STEPS.map((_, i) => (
            <div
              key={i}
              onClick={() => setStep(i)}
              style={{
                width: i === step ? '24px' : '8px',
                height: '8px',
                borderRadius: '4px',
                background: i === step ? '#667eea' : 'rgba(255,255,255,0.15)',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
              }}
            />
          ))}
        </div>

        {/* Footer */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          padding: '20px 28px',
          gap: '12px',
        }}>
          <button
            onClick={handleClose}
            style={{
              padding: '10px 20px',
              borderRadius: '8px',
              border: '1px solid rgba(255,255,255,0.15)',
              background: 'transparent',
              color: '#999',
              cursor: 'pointer',
              fontSize: '14px',
            }}
          >
            Skip
          </button>
          <div style={{ display: 'flex', gap: '10px' }}>
            {!isFirst && (
              <button
                onClick={() => setStep(s => s - 1)}
                style={{
                  padding: '10px 20px',
                  borderRadius: '8px',
                  border: '1px solid rgba(102,126,234,0.4)',
                  background: 'transparent',
                  color: '#667eea',
                  cursor: 'pointer',
                  fontSize: '14px',
                }}
              >
                Back
              </button>
            )}
            <button
              onClick={isLast ? handleClose : () => setStep(s => s + 1)}
              style={{
                padding: '10px 24px',
                borderRadius: '8px',
                border: 'none',
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                color: '#fff',
                cursor: 'pointer',
                fontSize: '14px',
                fontWeight: 600,
              }}
            >
              {isLast ? 'Get Started' : 'Next'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
