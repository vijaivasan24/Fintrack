export default function BudgetProgressBar({ spent, limit, exceeded }) {
  const numericSpent = typeof spent === 'number' ? spent : Number(spent) || 0;
  const numericLimit = typeof limit === 'number' ? limit : Number(limit) || 0;

  const percentage = numericLimit > 0
    ? Math.round((numericSpent / numericLimit) * 100)
    : 0;

  const isExceeded = Boolean(exceeded || (numericLimit > 0 && numericSpent > numericLimit));
  const fillWidth = Math.min(percentage, 100);

  return (
    <div className="w-full space-y-1">
      <div className="flex justify-between text-xs text-gray-500">
        <span>{percentage}% used</span>
        {isExceeded && (
          <span className="text-red-600 font-semibold">Exceeded</span>
        )}
      </div>
      <div className="w-full bg-gray-200 rounded-full h-2.5 overflow-hidden">
        <div
          data-testid="budget-progress-fill"
          className={`h-2.5 rounded-full transition-all duration-300 ${
            isExceeded ? 'bg-red-600' : 'bg-blue-600'
          }`}
          style={{ width: `${fillWidth}%` }}
        />
      </div>
    </div>
  );
}
