import React from 'react';

export function EquipmentStatusBadge({ status }) {
  const configs = {
    AVAILABLE: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500', label: 'Available' },
    BOOKED: { bg: 'bg-blue-100', text: 'text-blue-800', dot: 'bg-blue-500', label: 'Booked' },
    UNDER_MAINTENANCE: { bg: 'bg-amber-100', text: 'text-amber-800', dot: 'bg-amber-500', label: 'Under Maintenance' },
    OUT_OF_SERVICE: { bg: 'bg-rose-100', text: 'text-rose-800', dot: 'bg-rose-500', label: 'Out of Service' },
    RETIRED: { bg: 'bg-slate-200', text: 'text-slate-700', dot: 'bg-slate-500', label: 'Retired' },
  };

  const config = configs[status] || configs.AVAILABLE;

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium ${config.bg} ${config.text}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`}></span>
      {config.label}
    </span>
  );
}

export function BookingStatusBadge({ status }) {
  const configs = {
    CONFIRMED: { bg: 'bg-emerald-100', text: 'text-emerald-800', label: 'Confirmed' },
    PENDING_APPROVAL: { bg: 'bg-amber-100', text: 'text-amber-800', label: 'Pending Approval' },
    IN_USE: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'In Use' },
    COMPLETED: { bg: 'bg-slate-100', text: 'text-slate-700', label: 'Completed' },
    CANCELLED: { bg: 'bg-rose-100', text: 'text-rose-800', label: 'Cancelled' },
    NO_SHOW: { bg: 'bg-purple-100', text: 'text-purple-800', label: 'No Show' },
  };

  const config = configs[status] || configs.PENDING_APPROVAL;

  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold ${config.bg} ${config.text}`}>
      {config.label}
    </span>
  );
}
