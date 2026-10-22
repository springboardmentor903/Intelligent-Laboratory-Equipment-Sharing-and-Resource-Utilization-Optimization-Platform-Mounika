import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { Mail, Key, Lock, CheckCircle2, AlertCircle, ArrowLeft } from 'lucide-react';

export default function ForgotPassword() {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRequestToken = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const res = await api.post('/auth/password-reset-request', { email });
      setMessage(res.message || 'Reset token generated successfully.');
      // Extract token if returned in message or data
      const tokenMatch = res.message?.match(/Token:\s*([a-f0-9-]+)/i);
      if (tokenMatch) {
        setToken(tokenMatch[1]);
      }
      setStep(2);
    } catch (err) {
      setError(err.message || 'Failed to request password reset.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmReset = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const res = await api.post('/auth/password-reset-confirm', { token, newPassword });
      setMessage(res.message || 'Password reset confirmed! You can now sign in.');
      setStep(3);
    } catch (err) {
      setError(err.message || 'Failed to reset password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-slate-50">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="text-center text-3xl font-extrabold text-slate-900 tracking-tight">
          Reset Password
        </h2>
        <p className="mt-2 text-center text-sm text-slate-600">
          <Link to="/login" className="inline-flex items-center text-teal-700 hover:text-teal-800 font-medium">
            <ArrowLeft className="w-4 h-4 mr-1" /> Back to Sign In
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-sm border border-slate-200 sm:rounded-2xl sm:px-10">
          {error && (
            <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-lg flex items-center space-x-2">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {message && (
            <div className="mb-4 p-3 bg-teal-50 border border-teal-200 text-teal-800 text-sm rounded-lg flex items-center space-x-2">
              <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
              <span>{message}</span>
            </div>
          )}

          {step === 1 && (
            <form onSubmit={handleRequestToken} className="space-y-4">
              <p className="text-sm text-slate-600 mb-2">
                Enter your institutional email address to generate a secure password reset token.
              </p>
              <div>
                <label className="block text-sm font-medium text-slate-700">Email Address</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Mail className="h-5 w-5" />
                  </div>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="researcher@lab.edu"
                    className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-600 sm:text-sm"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 rounded-lg text-sm font-medium text-white bg-teal-700 hover:bg-teal-800 transition disabled:opacity-50"
              >
                {loading ? 'Sending Request...' : 'Send Reset Token'}
              </button>
            </form>
          )}

          {step === 2 && (
            <form onSubmit={handleConfirmReset} className="space-y-4">
              <p className="text-sm text-slate-600 mb-2">
                Enter the verification token and choose a new password.
              </p>

              <div>
                <label className="block text-sm font-medium text-slate-700">Reset Token</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Key className="h-5 w-5" />
                  </div>
                  <input
                    type="text"
                    required
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
                    className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-600 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">New Password</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Lock className="h-5 w-5" />
                  </div>
                  <input
                    type="password"
                    required
                    minLength={6}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-600 sm:text-sm"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 rounded-lg text-sm font-medium text-white bg-teal-700 hover:bg-teal-800 transition disabled:opacity-50"
              >
                {loading ? 'Resetting Password...' : 'Confirm New Password'}
              </button>
            </form>
          )}

          {step === 3 && (
            <div className="text-center py-4">
              <CheckCircle2 className="w-12 h-12 text-teal-600 mx-auto mb-3" />
              <h3 className="text-lg font-bold text-slate-800 mb-2">Password Successfully Changed</h3>
              <p className="text-sm text-slate-600 mb-6">
                Your password has been updated. You can now access your account with the new credentials.
              </p>
              <Link
                to="/login"
                className="inline-block py-2.5 px-6 rounded-lg text-sm font-medium text-white bg-teal-700 hover:bg-teal-800 transition shadow-sm"
              >
                Go to Sign In
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
