import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Beaker, Calendar, CheckSquare, User, LogOut, Shield } from 'lucide-react';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isManagerOrAdmin = user && [
    'ROLE_LAB_MANAGER',
    'ROLE_DEPARTMENT_HEAD',
    'ROLE_INSTITUTION_ADMIN',
    'ROLE_SYSTEM_ADMIN'
  ].includes(user.role);

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center space-x-8">
            <Link to="/" className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-gradient-to-tr from-teal-700 to-teal-500 rounded-xl flex items-center justify-center text-white shadow-sm">
                <Beaker className="w-6 h-6" />
              </div>
              <div>
                <span className="text-lg font-bold text-slate-900 tracking-tight">LabResource</span>
                <span className="text-xs block text-teal-700 font-semibold tracking-wider uppercase">Optimization Platform</span>
              </div>
            </Link>

            {/* Main Nav Links */}
            <div className="hidden md:flex space-x-2">
              <Link
                to="/"
                className={`px-3 py-2 rounded-lg text-sm font-medium transition ${
                  isActive('/') ? 'bg-teal-50 text-teal-800 font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Equipment Catalog
              </Link>

              {isAuthenticated && (
                <Link
                  to="/my-bookings"
                  className={`flex items-center space-x-1.5 px-3 py-2 rounded-lg text-sm font-medium transition ${
                    isActive('/my-bookings') ? 'bg-teal-50 text-teal-800 font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <Calendar className="w-4 h-4" />
                  <span>My Reservations</span>
                </Link>
              )}

              {isManagerOrAdmin && (
                <Link
                  to="/pending-approvals"
                  className={`flex items-center space-x-1.5 px-3 py-2 rounded-lg text-sm font-medium transition ${
                    isActive('/pending-approvals') ? 'bg-teal-50 text-teal-800 font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <CheckSquare className="w-4 h-4" />
                  <span>Approvals Queue</span>
                </Link>
              )}
            </div>
          </div>

          {/* Right Action Area */}
          <div className="flex items-center space-x-4">
            {isAuthenticated ? (
              <div className="flex items-center space-x-3">
                {/* Role Pill */}
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-sm font-semibold text-slate-800 leading-tight">{user.fullName}</span>
                  <span className="text-xs text-teal-700 font-medium inline-flex items-center justify-end gap-1">
                    <Shield className="w-3 h-3" />
                    {user.role?.replace('ROLE_', '').replace('_', ' ')}
                  </span>
                </div>

                {/* Profile link */}
                <Link
                  to="/profile"
                  className="p-2 text-slate-500 hover:text-teal-700 hover:bg-slate-100 rounded-lg transition"
                  title="View Profile"
                >
                  <User className="w-5 h-5" />
                </Link>

                {/* Logout Button */}
                <button
                  onClick={handleLogout}
                  className="flex items-center space-x-1.5 px-3 py-1.5 text-sm font-medium text-rose-600 hover:bg-rose-50 rounded-lg transition border border-rose-200"
                  title="Sign out"
                >
                  <LogOut className="w-4 h-4" />
                  <span className="hidden sm:inline">Logout</span>
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-medium text-slate-700 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 text-sm font-medium text-white bg-teal-700 hover:bg-teal-800 rounded-lg transition shadow-sm"
                >
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
