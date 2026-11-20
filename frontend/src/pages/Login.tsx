import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Eye, EyeOff, LockKeyhole, Package, UserRound } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { roleHomePath } from "../utils/roleHome";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const routeState =
    typeof location.state === "object" && location.state !== null
      ? (location.state as { registrationSuccess?: unknown })
      : undefined;
  const registrationSuccess =
    typeof routeState?.registrationSuccess === "string"
      ? routeState.registrationSuccess
      : "";

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      const response = await login(username.trim(), password);
      navigate(roleHomePath(response.roles || []));
    } catch (err: unknown) {
      const message =
        typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;

      setError(message || "Login failed. Please check your username and password.");
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
          <h1>From every order<br />to every detail.</h1>
          <p>
            Manage inventory, production, sales and finances
            in one connected workspace.
          </p>
          <div className="visual-points">
            <span><LockKeyhole size={16} /> Secure role-based access</span>
            <span><Package size={16} /> Connected inventory management</span>
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

          <span className="eyebrow">WELCOME BACK</span>
          <h2>Sign in to your account</h2>
          <p className="form-subtitle">Enter your credentials to continue.</p>

          {registrationSuccess && (
            <div className="registration-success" role="status">
              {registrationSuccess}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <label htmlFor="username">Username</label>
            <div className="field-wrap">
              <UserRound size={18} />
              <input
                id="username"
                type="text"
                placeholder="Enter your username"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
              />
            </div>

            <label htmlFor="password">Password</label>
            <div className="field-wrap">
              <LockKeyhole size={18} />
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="Enter your password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            {error && <div className="login-error" role="alert">{error}</div>}

            <button className="login-submit" type="submit" disabled={submitting}>
              {submitting ? "Signing in..." : "Sign in"}
            </button>
          </form>

          <p className="login-note auth-switch">
            New to OrdertoAudit? <Link to="/register">Create Account</Link>
          </p>
          <div className="login-copyright">© 2026 OrdertoAudit · Business Management</div>
        </div>
      </section>
    </main>
  );
}
