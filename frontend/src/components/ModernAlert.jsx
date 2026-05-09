import { useState, useRef, useCallback, createContext, useContext } from 'react';
import '../modern-alerts.css';

// Context for global alert access
const AlertContext = createContext(null);

export function useModernAlert() {
  return useContext(AlertContext);
}

// Individual Modal Component
function AlertModal({ alert, onClose }) {
  const { id, icon, title, message, modalClass, type, onConfirm, onCancel } = alert;

  const handleClose = () => {
    onClose(id);
  };

  const handleConfirm = () => {
    onClose(id);
    if (onConfirm) setTimeout(onConfirm, 300);
  };

  const handleCancel = () => {
    onClose(id);
    if (onCancel) setTimeout(onCancel, 300);
  };

  return (
    <div className="modern-alert-backdrop" onClick={type === 'alert' ? handleClose : undefined}>
      <div className={`modern-alert-modal ${modalClass || 'modal-info'}`} onClick={(e) => e.stopPropagation()}>
        <div className="modern-alert-header">
          <h5 className="modern-alert-title" dangerouslySetInnerHTML={{ __html: title }} />
        </div>
        <div className="modern-alert-body">
          <div className="modern-alert-icon">{icon}</div>
          <div className="modern-alert-message" dangerouslySetInnerHTML={{ __html: message }} />
        </div>
        <div className="modern-alert-footer">
          {type === 'confirm' ? (
            <>
              <button className="btn-cancel-modal" onClick={handleCancel}>No, Keep It</button>
              <button className="btn-confirm-modal" onClick={handleConfirm}>Yes, Confirm</button>
            </>
          ) : (
            <button className="btn-close-modal" onClick={handleClose}>Close</button>
          )}
        </div>
      </div>
    </div>
  );
}

// Provider Component
export function ModernAlertProvider({ children }) {
  const [alerts, setAlerts] = useState([]);
  // `let idCounter = 0` was reset on every render, defeating the whole point
  // of the counter. Storing it on a ref keeps the value stable across renders
  // (and out of the dependency list, since refs are mutable but stable).
  const idCounter = useRef(0);

  const removeAlert = useCallback((id) => {
    setAlerts(prev => prev.filter(a => a.id !== id));
  }, []);

  const showAlert = useCallback((icon, title, message, modalClass) => {
    const id = Date.now() + (++idCounter.current);
    setAlerts(prev => [...prev, { id, icon, title, message, modalClass, type: 'alert' }]);
  }, []);

  const showConfirm = useCallback((icon, title, message, modalClass, onConfirm, onCancel) => {
    const id = Date.now() + (++idCounter.current);
    setAlerts(prev => [...prev, { id, icon, title, message, modalClass, type: 'confirm', onConfirm, onCancel }]);
  }, []);

  // Predefined alert types matching ASP.NET ModernAlerts object
  const success = useCallback((title, message) => {
    showAlert('\u2705', title, message, 'modal-success');
  }, [showAlert]);

  const error = useCallback((title, message) => {
    showAlert('\u274C', title, message, 'modal-error');
  }, [showAlert]);

  const warning = useCallback((title, message) => {
    showAlert('\u26A0\uFE0F', title, message, 'modal-warning');
  }, [showAlert]);

  const info = useCallback((title, message) => {
    showAlert('\u2139\uFE0F', title, message, 'modal-info');
  }, [showAlert]);

  const confirmCancelBooking = useCallback((onConfirm, onCancel) => {
    showConfirm(
      '\u2753',
      'Cancel Booking?',
      'Are you sure you want to cancel this booking?<br><br>You will receive a <strong>6 credit refund</strong> and the parking spot will be released.',
      'modal-confirm',
      onConfirm,
      onCancel || (() => {})
    );
  }, [showConfirm]);

  const confirmDeleteBooking = useCallback((onConfirm, onCancel) => {
    showConfirm(
      '\uD83D\uDDD1\uFE0F',
      'Delete Booking?',
      'Are you sure you want to delete this booking?<br><br>This action <strong>cannot be undone</strong>. The parking spot will be released and credits may be refunded to the customer.',
      'modal-error',
      onConfirm,
      onCancel || (() => {})
    );
  }, [showConfirm]);

  const confirmReportSpotTaken = useCallback((onConfirm, onCancel) => {
    showConfirm(
      '\u26A0\uFE0F',
      'Report Spot Taken?',
      'Are you sure your assigned parking spot is occupied by another vehicle?<br><br>You will be assigned a <strong>new parking spot</strong> at no additional charge.',
      'modal-warning',
      onConfirm,
      onCancel || (() => {})
    );
  }, [showConfirm]);

  const confirmDeleteCustomer = useCallback((onConfirm, onCancel) => {
    showConfirm(
      '\u26A0\uFE0F',
      'Delete Customer?',
      'Are you sure you want to delete this customer?<br><br>This action <strong>cannot be undone</strong>.',
      'modal-error',
      onConfirm,
      onCancel || (() => {})
    );
  }, [showConfirm]);

  const value = {
    showAlert,
    showConfirm,
    success,
    error,
    warning,
    info,
    confirmCancelBooking,
    confirmDeleteBooking,
    confirmReportSpotTaken,
    confirmDeleteCustomer
  };

  return (
    <AlertContext.Provider value={value}>
      {children}
      {alerts.map(alert => (
        <AlertModal key={alert.id} alert={alert} onClose={removeAlert} />
      ))}
    </AlertContext.Provider>
  );
}
