export function roleHomePath(roles: string[]): string {
  if (roles.includes("ADMIN") || roles.includes("OWNER")) return "/dashboard";
  if (roles.includes("SALESMAN")) return "/orders";
  if (roles.includes("PRODUCTION")) return "/inventory";
  if (roles.includes("ACCOUNTANT")) return "/invoices";
  return "/login";
}
