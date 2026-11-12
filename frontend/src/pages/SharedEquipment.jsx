import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import {
  canRequestSharedAccess,
  canManageSharing,
  isInstitutionAdmin,
  isSystemAdmin
} from '../utils/roleHelper';
import {
  Share2,
  Search,
  Building,
  MapPin,
  Clock,
  Shield,
  FileText,
  DollarSign,
  AlertCircle,
  CheckCircle2,
  X,
  ExternalLink,
  Layers,
  ArrowRight,
  ListOrdered
} from 'lucide-react';

export default function SharedEquipment() {
  const { user, isAuthenticated } = useAuth();

  const [listings, setListings] = useState([]);
  const [institutions, setInstitutions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [searchKeyword, setSearchKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedInstitution, setSelectedInstitution] = useState('');

  // Access Request Modal State
  const [selectedListing, setSelectedListing] = useState(null);
  const [requestPurpose, setRequestPurpose] = useState('');
  const [estimatedHours, setEstimatedHours] = useState(10);
  const [submittingRequest, setSubmittingRequest] = useState(false);
  const [requestSuccess, setRequestSuccess] = useState('');
  const [requestError, setRequestError] = useState('');

  const fetchListings = () => {
    setLoading(true);
    api.get('/sharing/listings', {
      params: {
        institutionId: selectedInstitution || undefined,
        category: selectedCategory || undefined,
        keyword: searchKeyword || undefined,
      },
    })
      .then((res) => {
        if (res.success && res.data) {
          setListings(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load shared equipment listings.'))
      .finally(() => setLoading(false));
  };

  const fetchInstitutions = () => {
    api.get('/institutions')
      .then((res) => {
        if (res.success && res.data) {
          setInstitutions(res.data);
        }
      })
      .catch((err) => console.warn('Failed to load institutions list', err));
  };

  useEffect(() => {
    fetchInstitutions();
  }, []);

  useEffect(() => {
    fetchListings();
  }, [selectedInstitution, selectedCategory]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchListings();
  };

  const handleOpenRequestModal = (listing) => {
    setSelectedListing(listing);
    setRequestPurpose('');
    setEstimatedHours(10);
    setRequestSuccess('');
    setRequestError('');
  };

  const handleSubmitAccessRequest = async (e) => {
    e.preventDefault();
    if (!selectedListing) return;

    setSubmittingRequest(true);
    setRequestError('');
    setRequestSuccess('');

    try {
      const res = await api.post('/sharing/requests', {
        listingId: selectedListing.id,
        purpose: requestPurpose,
        estimatedUsageHours: Number(estimatedHours) || 10,
      });

      if (res.success) {
        setRequestSuccess(
          'Your access request was successfully submitted! It has been routed to your Home Institution Administrator for endorsement.'
        );
        setTimeout(() => {
          setSelectedListing(null);
          setRequestSuccess('');
        }, 3000);
      }
    } catch (err) {
      setRequestError(err.message || 'Failed to submit inter-institution access request.');
    } finally {
      setSubmittingRequest(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-8">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-teal-900 via-indigo-900 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-md relative overflow-hidden">
        <div className="relative z-10 max-w-3xl space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-teal-500/20 text-teal-300 text-xs font-semibold uppercase tracking-wider border border-teal-500/30">
            <Share2 className="w-3.5 h-3.5" />
            Inter-Institution Resource Sharing
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Cross-Institution Shared Laboratories &amp; Instrumentation
          </h1>
          <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
            Discover and access advanced laboratory equipment shared by partner universities and research institutions.
            Apply for access grants through a verified two-step institutional approval workflow.
          </p>

          {/* Quick Nav Pills */}
          <div className="pt-2 flex flex-wrap gap-2 text-xs">
            {isAuthenticated && canRequestSharedAccess(user) && (
              <>
                <Link
                  to="/access-requests"
                  className="px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white font-medium transition flex items-center gap-1.5"
                >
                  <ListOrdered className="w-3.5 h-3.5 text-teal-300" />
                  <span>My Access Requests</span>
                </Link>
                <Link
                  to="/my-shared-access"
                  className="px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white font-medium transition flex items-center gap-1.5"
                >
                  <Shield className="w-3.5 h-3.5 text-teal-300" />
                  <span>My Active Grants</span>
                </Link>
              </>
            )}

            {isAuthenticated && canManageSharing(user) && (
              <>
                <Link
                  to="/access-requests"
                  className="px-3 py-1.5 rounded-lg bg-teal-700/80 hover:bg-teal-600 text-white font-medium transition flex items-center gap-1.5"
                >
                  <Layers className="w-3.5 h-3.5" />
                  <span>Review Access Requests</span>
                </Link>
                <Link
                  to="/agreements"
                  className="px-3 py-1.5 rounded-lg bg-indigo-700/80 hover:bg-indigo-600 text-white font-medium transition flex items-center gap-1.5"
                >
                  <FileText className="w-3.5 h-3.5" />
                  <span>Bilateral Agreements</span>
                </Link>
                <Link
                  to="/sharing-analytics"
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-medium transition flex items-center gap-1.5 border border-slate-700"
                >
                  <Share2 className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Partnership Analytics</span>
                </Link>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-4 sm:p-6">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-12 gap-3">
          <div className="sm:col-span-5 relative">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="Search shared equipment, category, manufacturer..."
              className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-600 focus:bg-white transition"
            />
          </div>

          <div className="sm:col-span-3">
            <select
              value={selectedInstitution}
              onChange={(e) => setSelectedInstitution(e.target.value)}
              className="w-full py-2 px-3 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-600 focus:bg-white"
            >
              <option value="">All Partner Institutions</option>
              {institutions.map((inst) => (
                <option key={inst.id} value={inst.id}>
                  {inst.name}
                </option>
              ))}
            </select>
          </div>

          <div className="sm:col-span-2">
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full py-2 px-3 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-600 focus:bg-white"
            >
              <option value="">All Categories</option>
              <option value="Spectroscopy">Spectroscopy</option>
              <option value="Chromatography">Chromatography</option>
              <option value="Microscopy">Microscopy</option>
              <option value="Thermal Analysis">Thermal Analysis</option>
              <option value="Diffraction">Diffraction</option>
            </select>
          </div>

          <div className="sm:col-span-2 flex space-x-2">
            <button
              type="submit"
              className="flex-1 py-2 px-3 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-semibold transition"
            >
              Search
            </button>
            <button
              type="button"
              onClick={() => {
                setSearchKeyword('');
                setSelectedCategory('');
                setSelectedInstitution('');
              }}
              className="py-2 px-3 border border-slate-300 hover:bg-slate-50 text-slate-600 rounded-xl text-xs font-medium"
            >
              Reset
            </button>
          </div>
        </form>
      </div>

      {/* Listings Grid */}
      {loading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-600"></div>
        </div>
      ) : error ? (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-center text-sm">
          {error}
        </div>
      ) : listings.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <Share2 className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">No Shared Equipment Listings Found</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-md mx-auto">
            There are currently no inter-institution equipment listings matching your search filters.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {listings.map((listing) => {
            const eq = listing.equipment;
            const hostInst = eq.department?.institution?.name || eq.institution?.name || 'Partner Institution';
            const isOwnInstitution = user?.department?.institution?.id &&
              (eq.department?.institution?.id === user.department.institution.id);

            return (
              <div
                key={listing.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition overflow-hidden flex flex-col"
              >
                <div className="p-5 flex-1 space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[11px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-teal-50 text-teal-800 border border-teal-100">
                      {eq.category}
                    </span>
                    <span className="text-xs font-extrabold text-slate-900 bg-slate-100 px-2 py-0.5 rounded-md">
                      ${listing.feePerHour}/hr
                    </span>
                  </div>

                  <h3 className="text-base font-bold text-slate-900 leading-snug">
                    <Link to={`/equipment/${eq.id}`} className="hover:text-teal-700 transition">
                      {eq.name}
                    </Link>
                  </h3>

                  <div className="space-y-1 text-xs text-slate-600">
                    <div className="flex items-center">
                      <Building className="w-3.5 h-3.5 mr-1.5 text-teal-600 flex-shrink-0" />
                      <span className="font-semibold text-slate-800">{hostInst}</span>
                    </div>
                    <div className="flex items-center pl-5 text-slate-500">
                      <span>{eq.department?.name || 'Department'} • {eq.locationRoom || 'Lab'}</span>
                    </div>
                  </div>

                  <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                    {eq.description}
                  </p>

                  {/* Sharing Terms Badges */}
                  <div className="pt-2 border-t border-slate-100 grid grid-cols-3 gap-1 text-[10px] text-center">
                    <div className="p-1.5 bg-slate-50 rounded-lg">
                      <span className="text-slate-400 block">Min Notice</span>
                      <span className="font-bold text-slate-700">{listing.minNoticeHours} hrs</span>
                    </div>
                    <div className="p-1.5 bg-slate-50 rounded-lg">
                      <span className="text-slate-400 block">Max Session</span>
                      <span className="font-bold text-slate-700">{listing.maxBookingHours} hrs</span>
                    </div>
                    <div className="p-1.5 bg-slate-50 rounded-lg">
                      <span className="text-slate-400 block">Share Cap</span>
                      <span className="font-bold text-teal-700">{listing.externalSharePercentage}% / wk</span>
                    </div>
                  </div>

                  {listing.termsAndConditions && (
                    <div className="p-2 bg-indigo-50/50 rounded-lg border border-indigo-100 text-[11px] text-indigo-900">
                      <span className="font-semibold block mb-0.5 flex items-center gap-1">
                        <FileText className="w-3 h-3 text-indigo-600" />
                        Access Terms Preview:
                      </span>
                      <p className="line-clamp-2 text-indigo-800 text-[10px] italic">
                        {listing.termsAndConditions}
                      </p>
                    </div>
                  )}
                </div>

                {/* Footer Action */}
                <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                  <Link
                    to={`/equipment/${eq.id}`}
                    className="text-xs font-medium text-slate-600 hover:text-slate-900 flex items-center gap-1"
                  >
                    <span>View Calendar</span>
                    <ExternalLink className="w-3 h-3" />
                  </Link>

                  {isAuthenticated ? (
                    isOwnInstitution ? (
                      <span className="text-xs text-slate-400 italic">Internal Resource</span>
                    ) : (
                      <button
                        onClick={() => handleOpenRequestModal(listing)}
                        className="px-3 py-1.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-semibold shadow-sm transition flex items-center gap-1"
                      >
                        <Shield className="w-3.5 h-3.5" />
                        <span>Request Access</span>
                      </button>
                    )
                  ) : (
                    <Link
                      to="/login"
                      className="px-3 py-1.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-semibold shadow-sm transition"
                    >
                      Sign In
                    </Link>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Request Access Modal */}
      {selectedListing && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200">
            <div className="flex justify-between items-center pb-3 border-b border-slate-200">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Share2 className="w-4 h-4 text-teal-700" />
                Inter-Institution Equipment Access Application
              </h3>
              <button onClick={() => setSelectedListing(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {requestSuccess && (
              <div className="my-3 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
                <span>{requestSuccess}</span>
              </div>
            )}

            {requestError && (
              <div className="my-3 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center space-x-2">
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                <span>{requestError}</span>
              </div>
            )}

            <form onSubmit={handleSubmitAccessRequest} className="mt-4 space-y-4">
              {/* Selected Equipment Summary */}
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
                <div>
                  <span className="text-slate-500 font-medium">Target Equipment:</span>{' '}
                  <span className="font-bold text-slate-900">{selectedListing.equipment.name}</span>
                </div>
                <div>
                  <span className="text-slate-500 font-medium">Host Institution:</span>{' '}
                  <span className="font-semibold text-slate-800">
                    {selectedListing.equipment.department?.institution?.name || 'Host University'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 font-medium">Usage Fee Rate:</span>{' '}
                  <span className="font-bold text-teal-700">${selectedListing.feePerHour} / hour</span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-800 mb-1">
                  Research Project Purpose &amp; Methodology *
                </label>
                <textarea
                  rows={3}
                  required
                  value={requestPurpose}
                  onChange={(e) => setRequestPurpose(e.target.value)}
                  placeholder="Detail your scientific investigation, protocol, sample types, and why this specific facility is required..."
                  className="w-full p-2.5 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-600"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-800 mb-1">
                  Estimated Total Usage Hours Needed *
                </label>
                <input
                  type="number"
                  min="1"
                  max="500"
                  required
                  value={estimatedHours}
                  onChange={(e) => setEstimatedHours(e.target.value)}
                  className="w-full p-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-600"
                />
                <span className="text-[11px] text-slate-400 mt-0.5 block">
                  Approximate total machine hours planned across all experimental sessions.
                </span>
              </div>

              {/* Two-step approval note */}
              <div className="p-3 bg-teal-50 border border-teal-200 rounded-xl text-[11px] text-teal-900 space-y-1">
                <span className="font-bold block">Two-Step Verification Process:</span>
                <p>1. Your Home Institution Admin reviews and endorses this request.</p>
                <p>2. The Host Institution Admin reviews and grants an active Access Grant.</p>
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setSelectedListing(null)}
                  className="px-3.5 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingRequest || !requestPurpose.trim()}
                  className="px-4 py-1.5 bg-teal-700 text-white rounded-xl text-xs font-semibold hover:bg-teal-800 disabled:opacity-50"
                >
                  {submittingRequest ? 'Submitting Application...' : 'Submit Access Application'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
