import { formatCurrency, SHORT_MONTHS } from '../utils/formatters';

export default function YearlyBarChart({
  year,
  onYearChange,
  monthlyData,
  loading,
  error,
}) {
  const baseYear = year || 2026;
  const yearOptions = [
    baseYear - 2,
    baseYear - 1,
    baseYear,
    baseYear + 1,
    baseYear + 2,
  ];

  // Map 12 months
  const monthsData = Array.from({ length: 12 }, (_, index) => {
    const monthNum = index + 1;
    const found = Array.isArray(monthlyData)
      ? monthlyData.find((m) => m.month === monthNum)
      : null;

    return {
      month: monthNum,
      name: SHORT_MONTHS[index],
      income: Number(found?.totalIncome || found?.income) || 0,
      expense: Number(found?.totalExpense || found?.expense) || 0,
      net: Number(found?.netBalance) || 0,
    };
  });

  const maxAmount = Math.max(
    ...monthsData.map((m) => Math.max(m.income, m.expense)),
    1000
  );

  const hasAnyData = monthsData.some((m) => m.income > 0 || m.expense > 0);

  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
        <div>
          <h2 className="text-base font-semibold text-gray-900">
            Income vs Expense ({year})
          </h2>
          <p className="text-xs text-gray-500 mt-0.5">
            12-month annual comparison
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-4 text-xs">
            <span className="inline-flex items-center">
              <span className="w-3 h-3 bg-green-500 rounded-sm mr-1.5" />
              Income
            </span>
            <span className="inline-flex items-center">
              <span className="w-3 h-3 bg-red-500 rounded-sm mr-1.5" />
              Expense
            </span>
          </div>

          <div className="flex items-center space-x-1.5">
            <label htmlFor="chart-year" className="text-xs text-gray-600 font-medium">
              Year:
            </label>
            <select
              id="chart-year"
              value={year}
              onChange={(e) => onYearChange(Number(e.target.value))}
              className="px-2.5 py-1 text-xs border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
            >
              {yearOptions.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="h-64 flex items-center justify-center text-sm text-gray-500">
          Loading annual chart...
        </div>
      ) : error ? (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
          Failed to load annual chart: {error}
        </div>
      ) : !hasAnyData ? (
        <div className="h-64 flex flex-col items-center justify-center text-sm text-gray-500">
          <p>No financial activity recorded for {year}.</p>
        </div>
      ) : (
        <div className="h-64 pt-6 flex items-end justify-between gap-1 sm:gap-2 border-b border-gray-200 pb-2">
          {monthsData.map((m) => {
            const incomeHeight = maxAmount > 0
              ? Math.max((m.income / maxAmount) * 100, m.income > 0 ? 3 : 0)
              : 0;
            const expenseHeight = maxAmount > 0
              ? Math.max((m.expense / maxAmount) * 100, m.expense > 0 ? 3 : 0)
              : 0;

            return (
              <div
                key={m.month}
                className="flex-1 flex flex-col items-center h-full justify-end group relative"
              >
                {/* Tooltip on hover */}
                <div className="hidden group-hover:block absolute bottom-full mb-2 z-10 bg-gray-900 text-white text-[11px] p-2 rounded shadow-lg whitespace-nowrap pointer-events-none">
                  <div className="font-semibold mb-0.5">{m.name} {year}</div>
                  <div className="text-green-400">Income: {formatCurrency(m.income)}</div>
                  <div className="text-red-400">Expense: {formatCurrency(m.expense)}</div>
                  <div className="text-gray-300">Net: {formatCurrency(m.net)}</div>
                </div>

                <div className="w-full flex items-end justify-center space-x-1 h-48">
                  {/* Income bar */}
                  <div
                    data-testid={`bar-income-${m.month}`}
                    title={`Income: ${formatCurrency(m.income)}`}
                    style={{ height: `${incomeHeight}%` }}
                    className="w-2.5 sm:w-4 bg-green-500 hover:bg-green-600 rounded-t transition-all duration-300"
                  />
                  {/* Expense bar */}
                  <div
                    data-testid={`bar-expense-${m.month}`}
                    title={`Expense: ${formatCurrency(m.expense)}`}
                    style={{ height: `${expenseHeight}%` }}
                    className="w-2.5 sm:w-4 bg-red-500 hover:bg-red-600 rounded-t transition-all duration-300"
                  />
                </div>

                <span className="text-[11px] text-gray-500 mt-2">{m.name}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
