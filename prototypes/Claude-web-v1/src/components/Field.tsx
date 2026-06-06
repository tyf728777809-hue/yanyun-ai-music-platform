import type { ReactNode, TextareaHTMLAttributes, InputHTMLAttributes } from 'react';

interface FieldShellProps {
  label: string;
  hint?: string;
  optional?: boolean;
  counter?: string;
  children: ReactNode;
}

function FieldShell({ label, hint, optional, counter, children }: FieldShellProps) {
  return (
    <label className="field">
      <span className="field__label">
        {label}
        {optional && <span className="field__optional">选填</span>}
      </span>
      {children}
      <span className="field__footrow">
        {hint && <span className="field__hint">{hint}</span>}
        {counter && <span className="field__counter">{counter}</span>}
      </span>
    </label>
  );
}

interface TextAreaFieldProps
  extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'className'> {
  label: string;
  hint?: string;
  optional?: boolean;
  maxLength?: number;
}

export function TextAreaField({ label, hint, optional, value, maxLength, ...rest }: TextAreaFieldProps) {
  const len = typeof value === 'string' ? value.length : 0;
  return (
    <FieldShell
      label={label}
      hint={hint}
      optional={optional}
      counter={maxLength ? `${len}/${maxLength}` : undefined}
    >
      <textarea className="field__textarea" value={value} maxLength={maxLength} {...rest} />
    </FieldShell>
  );
}

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'className'> {
  label: string;
  hint?: string;
  optional?: boolean;
  maxLength?: number;
}

export function TextField({ label, hint, optional, value, maxLength, ...rest }: TextFieldProps) {
  const len = typeof value === 'string' ? value.length : 0;
  return (
    <FieldShell
      label={label}
      hint={hint}
      optional={optional}
      counter={maxLength ? `${len}/${maxLength}` : undefined}
    >
      <input className="field__input" value={value} maxLength={maxLength} {...rest} />
    </FieldShell>
  );
}

interface ChoiceOption {
  value: string;
  label: string;
}

interface ChoiceFieldProps {
  label: string;
  optional?: boolean;
  options: ChoiceOption[];
  value: string;
  onChange: (value: string) => void;
}

// 分段选择（如声线偏好），比下拉更易扫描，移动端也好点。
export function ChoiceField({ label, optional, options, value, onChange }: ChoiceFieldProps) {
  return (
    <FieldShell label={label} optional={optional}>
      <div className="choice" aria-label={label}>
        {options.map((opt) => (
          <button
            key={opt.value}
            type="button"
            aria-pressed={value === opt.value}
            className={`choice__item ${value === opt.value ? 'is-selected' : ''}`}
            onClick={() => onChange(opt.value)}
          >
            {opt.label}
          </button>
        ))}
      </div>
    </FieldShell>
  );
}
