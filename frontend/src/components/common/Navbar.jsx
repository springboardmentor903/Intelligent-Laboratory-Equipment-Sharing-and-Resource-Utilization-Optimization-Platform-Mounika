import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  canApproveBookings,
  canBookEquipment,
  canViewUtilizationAnalytics,
  canManageSharing,
  canRequestSharedAccess,
  normalizeRole
} from '../../utils/roleHelper';
import {
  Beaker,
  Calendar,
  CheckSquare,
  User,
  LogOut,
  Shield,
  Activity,
  BarChart3,
  Share2,
  ListOrdered,
  Menu,
  X,
  FileText
} from 'lucide-react';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center space-x-6">
            <Link to="/" className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-gradient-to-tr from-teal-700 to-teal-500 rounded-xl flex items-center justify-center text-white shadow-sm">
                <Beaker className="w-6 h-6" />
              </div>
              <div>
                <span className="text-lg font-bold text-slate-900 tracking-tight">LabResource</span>
                <span className="text-xs block text-teal-700 font-semibold tracking-wider uppercase">Platform</span>
              </div>
            </Link>

            {/* Main Nav Links (Desktop) */}
            <div className="hidden lg:flex items-center space-x-1">
              <Link
                to="/"
                className={`px-3 py-2 rounded-lg text-xs font-semibold transition ${
                  isActive('/') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Catalog
              </Link>

              <Link
                to="/monitoring"
                className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                  isActive('/monitoring') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <Activity className="w-3.5 h-3.5" />
                <span>Live Monitor</span>
              </Link>

              {isAuthenticated && canBookEquipment(user) && (
                <Link
                  to="/my-bookings"
                  className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                    isActive('/my-bookings') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <Calendar className="w-3.5 h-3.5" />
                  <span>My Bookings</span>
                </Link>
              )}

              {isAuthenticated && canBookEquipment(user) && (
                <Link
                  to="/waitlist"
                  className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                    isActive('/waitlist') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <ListOrdered className="w-3.5 h-3.5" />
                  <span>My Waitlist</span>
                </Link>
              )}

              <Link
                to="/shared-equipment"
                className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                  isActive('/shared-equipment') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <Share2 className="w-3.5 h-3.5" />
                <span>Shared Resources</span>
              </Link>

              {isAuthenticated && canApproveBookings(user) && (
                <Link
                  to="/pending-approvals"
                  className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                    isActive('/pending-approvals') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <CheckSquare className="w-3.5 h-3.5" />
                  <span>Approvals</span>
                </Link>
              )}

              {isAuthenticated && canViewUtilizationAnalytics(user) && (
                <Link
                  to="/analytics"
                  className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                    isActive('/analytics') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <BarChart3 className="w-3.5 h-3.5" />
                  <span>Analytics</span>
                </Link>
              )}

              {isAuthenticated && canManageSharing(user) && (
                <>
                  <Link
                    to="/agreements"
                    className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                      isActive('/agreements') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    }`}
                  >
                    <FileText className="w-3.5 h-3.5 text-indigo-600" />
                    <span>Agreements</span>
                  </Link>

                  <Link
                    to="/sharing-analytics"
                    className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                      isActive('/sharing-analytics') ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    }`}
                  >
                    <Share2 className="w-3.5 h-3.5 text-indigo-600" />
                    <span>Partnerships</span>
                  </Link>
                </>
              )}
            </div>
          </div>

          {/* Right Action Area */}
          <div className="flex items-center space-x-3">
            {isAuthenticated ? (
              <div className="flex items-center space-x-3">
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-xs font-bold text-slate-800 leading-tight">{user.fullName}</span>
                  <span className="text-[10px] text-teal-700 font-semibold inline-flex items-center justify-end gap-0.5">
                    <Shield className="w-2.5 h-2.5" />
                    {normalizeRole(user.role).replace('_', ' ')}
                  </span>
                </div>

                <Link
                  to="/profile"
                  className="p-1.5 text-slate-500 hover:text-teal-700 hover:bg-slate-100 rounded-lg transition"
                  title="View Profile"
                >
                  <User className="w-4 h-4" />
                </Link>

                <button
                  onClick={handleLogout}
                  className="inline-flex items-center px-2.5 py-1.5 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-medium transition"
                  title="Sign out"
                >
                  <LogOut className="w-3.5 h-3.5 sm:mr-1" />
                  <span className="hidden sm:inline">Sign Out</span>
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Link
                  to="/login"
                  className="px-3 py-1.5 text-xs font-semibold text-slate-700 hover:text-slate-900"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-3.5 py-1.5 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-semibold shadow-sm transition"
                >
                  Register
                </Link>
              </div>
            )}

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile menu dropdown */}
        {mobileMenuOpen && (
          <div className="lg:hidden py-3 border-t border-slate-200 space-y-1">
            <Link
              to="/"
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Equipment Catalog
            </Link>
            <Link
              to="/monitoring"
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Live Equipment Monitor
            </Link>
            {isAuthenticated && canBookEquipment(user) && (
              <Link
                to="/my-bookings"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                My Reservations
              </Link>
            )}
            {isAuthenticated && canBookEquipment(user) && (
              <Link
                to="/waitlist"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                My Waitlist
              </Link>
            )}
            <Link
              to="/shared-equipment"
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Shared Resources
            </Link>
            {isAuthenticated && canApproveBookings(user) && (
              <Link
                to="/pending-approvals"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Approvals Queue
              </Link>
            )}
            {isAuthenticated && canViewUtilizationAnalytics(user) && (
              <Link
                to="/analytics"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Utilization Analytics
              </Link>
            )}
            {isAuthenticated && canManageSharing(user) && (
              <>
                <Link
                  to="/agreements"
                  onClick={() => setMobileMenuOpen(false)}
                  className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
                >
                  Bilateral Agreements
                </Link>
                <Link
                  to="/sharing-analytics"
                  onClick={() => setMobileMenuOpen(false)}
                  className="block px-3 py-2 rounded-md text-xs font-semibold text-slate-700 hover:bg-slate-50"
                >
                  Sharing & Partnerships
                </Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  );
}
