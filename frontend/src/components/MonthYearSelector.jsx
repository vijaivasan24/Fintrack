const MONTHS = [
  { value: 1, name: 'January' },
  { value: 2, name: 'February' },
  { value: 3, name: 'March' },
  { value: 4, name: 'April' },
  { value: 5, name: 'May' },
  { value: 6, name: 'June' },
  { value: 7, name: 'July' },
  { value: 8, name: 'August' },
  { value: 9, name: 'September' },
  { value: 10, name: 'October' },
  { value: 11, name: 'November' },
  { value: 12, name: 'December' },
];

export default function MonthYearSelector({ year, month, onChange }) {
  const baseYear = year || 2026;
  const yearOptions = [
    baseYear - 2,
    baseYear - 1,
    baseYear,
    baseYear + 1,
    baseYear + 2,
  ];

  return (
    <div className="flex flex-wrap items-center gap-3 bg-white p-3 rounded-lg border border-gray-200 shadow-sm">
      <div className="flex items-center space-x-2">
        <label htmlFor="select-month" className="text-xs font-medium text-gray-700">
          Month
        </label>
        <select
          id="select-month"
          value={month}
          onChange={(e) => onChange({ year, month: Number(e.target.value) })}
          className="px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
        >
          {MONTHS.map((m) => (
            <option key={m.value} value={m.value}>
              {m.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex items-center space-x-2">
        <label htmlFor="select-year" className="text-xs font-medium text-gray-700">
          Year
        </label>
        <select
          id="select-year"
          value={year}
          onChange={(e) => onChange({ year: Number(e.target.value), month })}
          className="px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
        >
          {yearOptions.map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
