import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import {
  Shield,
  Building,
  Calendar,
  Clock,
  ArrowRight,
  ExternalLink,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { format } from 'date-fns';

export default function MySharedAccess() {
  const [grants, setGrants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    api.get('/sharing/my-grants')
      .then((res) => {
        if (res.success && res.data) {
          setGrants(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load access grants.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Shield className="w-7 h-7 text-teal-700" />
            My Active Shared Access Grants
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Approved credentials allowing you to book and operate equipment at partner university laboratories.
          </p>
        </div>

        <Link
          to="/shared-equipment"
          className="inline-flex items-center px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-semibold shadow-sm transition space-x-1.5"
        >
          <span>Browse Shared Resources</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>

      {loading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-600"></div>
        </div>
      ) : error ? (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-center text-sm">
          {error}
        </div>
      ) : grants.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <Shield className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">No Active Access Grants</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-md mx-auto">
            You currently do not have any active inter-institution access grants. Browse the shared catalog to apply for access to external equipment.
          </p>
          <Link
            to="/shared-equipment"
            className="inline-flex items-center text-teal-700 font-semibold text-xs mt-4 hover:underline"
          >
            Explore Partner Laboratories &rarr;
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {grants.map((grant) => {
            const eq = grant.equipment;
            const hostInst = eq.department?.institution?.name || 'Partner University';

            return (
              <div
                key={grant.id}
                className="bg-white rounded-2xl border border-teal-200/80 shadow-sm p-5 space-y-4 hover:shadow-md transition flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-teal-800 bg-teal-50 px-2 py-0.5 rounded border border-teal-100">
                      {eq.category}
                    </span>
                    <span className="text-[11px] font-semibold text-emerald-700 flex items-center gap-1 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                      <CheckCircle2 className="w-3 h-3" />
                      Active Grant
                    </span>
                  </div>

                  <h3 className="text-base font-bold text-slate-900 leading-snug">
                    {eq.name}
                  </h3>

                  <div className="space-y-1 text-xs text-slate-600">
                    <div className="flex items-center">
                      <Building className="w-3.5 h-3.5 mr-1.5 text-teal-600 flex-shrink-0" />
                      <span className="font-semibold text-slate-800">{hostInst}</span>
                    </div>
                    <div className="pl-5 text-slate-500">
                      {eq.department?.name || 'Department'} • {eq.locationRoom || 'Lab'}
                    </div>
                  </div>

                  <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1">
                    <div className="flex items-center justify-between text-slate-600">
                      <span>Valid Window:</span>
                      <span className="font-semibold text-slate-800">
                        {grant.validFrom} to {grant.validTo}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-slate-600">
                      <span>Agreement Ref:</span>
                      <span className="font-mono text-[11px] text-slate-700 font-medium">
                        {grant.agreement?.agreementNumber}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-800">
                    ${grant.agreement?.feePerHour}/hr
                  </span>

                  <Link
                    to={`/equipment/${eq.id}`}
                    className="px-3.5 py-1.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-semibold shadow-sm transition flex items-center gap-1"
                  >
                    <span>Book Equipment</span>
                    <ArrowRight className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
