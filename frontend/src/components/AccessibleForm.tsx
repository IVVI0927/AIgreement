import React from 'react';

interface AccessibleFormProps {
  onSubmit: (e: React.FormEvent) => void;
  children: React.ReactNode;
  ariaLabel: string;
}

export const AccessibleForm: React.FC<AccessibleFormProps> = ({ onSubmit, children, ariaLabel }) => {
  return (
    <form 
      onSubmit={onSubmit}
      role="form"
      aria-label={ariaLabel}
      noValidate
    >
      {children}
    </form>
  );
};

interface AccessibleInputProps {
  id: string;
  label: string;
  type: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  required?: boolean;
  error?: string;
  helperText?: string;
}

export const AccessibleInput: React.FC<AccessibleInputProps> = ({
  id,
  label,
  type,
  value,
  onChange,
  required = false,
  error,
  helperText
}) => {
  return (
    <div className="form-group">
      <label htmlFor={id} className="form-label">
        {label}
        {required && <span aria-label="required" className="required">*</span>}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        className={`form-input ${error ? 'error' : ''}`}
        aria-invalid={!!error}
        aria-describedby={`${id}-helper ${id}-error`}
        aria-required={required}
      />
      {helperText && (
        <span id={`${id}-helper`} className="helper-text" role="status">
          {helperText}
        </span>
      )}
      {error && (
        <span id={`${id}-error`} className="error-text" role="alert" aria-live="polite">
          {error}
        </span>
      )}
    </div>
  );
};

export const SkipLink: React.FC = () => {
  return (
    <a href="#main-content" className="skip-link">
      Skip to main content
    </a>
  );
};