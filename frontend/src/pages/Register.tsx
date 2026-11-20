import axios from "axios";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  Package,
  UserRound,
} from "lucide-react";
import { registerUser } from "../api/auth";

interface RegistrationErrorBody {
  message?: string;
  validationErrors?: Record<string, string>;
}

function getRegistrationError(error: unknown): string {
  if (axios.isAxiosError<RegistrationErrorBody>(error)) {
    const response = error.response?.data;
    const validationMessages = response?.validationErrors
      ? Object.values(response.validationErrors).filter(Boolean)
      : [];

    if (validationMessages.length > 0) {
      return validationMessages.join(" ");
    }

    if (response?.message) {
      return response.message;
    }
  }

  return "Unable to create your account. Check your connection and try again.";
}

export default function Register() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    const normalizedName = fullName.trim();
    const normalizedUsername = username.trim();
    const normalizedEmail = email.trim();

    if (!normalizedName || !normalizedUsername || !normalizedEmail || !password.trim()) {
      setError("Please complete all required fields.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);

    try {
      const account = await registerUser({
        fullName: normalizedName,
        username: normalizedUsername,
        email: normalizedEmail,
        password,
      });

      navigate("/login", {
        replace: true,
        state: {
          registrationSuccess: `Account created for ${account.username}. You can sign in now.`,
        },
      });
    } catch (requestError: unknown) {
      setError(getRegistrationError(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-visual">
        <div className="brand">
          <div className="brand-icon"><Package size={26} /></div>
          <span>Order<span className="brand-accent">to</span>Audit</span>
        </div>

        <div className="login-visual-content">
          <span className="eyebrow">SMART BUSINESS MANAGEMENT</span>
          <h1>Bring every order<br />into focus.</h1>
          <p>
            Create your account to manage customer orders in one connected
            manufacturing workspace.
          </p>
          <div className="visual-points">
            <span><LockKeyhole size={16} /> Secure role-based access</span>
            <span><Package size={16} /> Sales workspace access</span>
          </div>
        </div>
        <div className="visual-footer">ORDER-TO-AUDIT MANAGEMENT SYSTEM</div>
      </section>

      <section className="login-form-section">
        <div className="login-form-wrap">
          <div className="mobile-brand">
            <div className="brand-icon"><Package size={24} /></div>
            <span>Order<span className="brand-accent">to</span>Audit</span>
          </div>

          <span className="eyebrow">GET STARTED</span>
          <h2>Create your account</h2>
          <p className="form-subtitle">
            Register for access to the Salesman workspace.
          </p>

          <form onSubmit={handleSubmit}>
            <label htmlFor="fullName">Full Name</label>
            <div className="field-wrap">
              <UserRound size={18} />
              <input
                id="fullName"
                name="fullName"
                type="text"
                placeholder="Enter your full name"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                autoComplete="name"
                required
              />
            </div>

            <label htmlFor="registerUsername">Username</label>
            <div className="field-wrap">
              <UserRound size={18} />
              <input
                id="registerUsername"
                name="username"
                type="text"
                placeholder="Choose a username"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
              />
            </div>

            <label htmlFor="registerEmail">Email</label>
            <div className="field-wrap">
              <Mail size={18} />
              <input
                id="registerEmail"
                name="email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                required
              />
            </div>

            <label htmlFor="registerPassword">Password</label>
            <div className="field-wrap">
              <LockKeyhole size={18} />
              <input
                id="registerPassword"
                name="password"
                type={showPassword ? "text" : "password"}
                placeholder="Create a password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="new-password"
                required
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            <label htmlFor="confirmPassword">Confirm Password</label>
            <div className="field-wrap">
              <LockKeyhole size={18} />
              <input
                id="confirmPassword"
                name="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                placeholder="Enter your password again"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                autoComplete="new-password"
                required
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setShowConfirmPassword((visible) => !visible)}
                aria-label={showConfirmPassword ? "Hide confirmation" : "Show confirmation"}
              >
                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            {error && <div className="login-error" role="alert">{error}</div>}

            <button className="login-submit" type="submit" disabled={submitting}>
              {submitting ? "Creating account..." : "Create Account"}
            </button>
          </form>

          <p className="login-note auth-switch">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
          <div className="login-copyright">© 2026 OrdertoAudit · Business Management</div>
        </div>
      </section>
    </main>
  );
}
