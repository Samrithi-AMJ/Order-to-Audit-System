import api from "./axios";

export interface AuthResponse {
  token: string;
  tokenType: string;
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
}

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
}

export interface RegistrationInput {
  fullName: string;
  username: string;
  email: string;
  password: string;
}

export interface RegistrationResponse {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  active: boolean;
  roles: string[];
}

export const registerUser = async (
  input: RegistrationInput,
): Promise<RegistrationResponse> => {
  const response = await api.post<RegistrationResponse>("/auth/register", {
    ...input,
    // The backend requires this field, then independently enforces SALESMAN.
    roles: ["SALESMAN"],
  });

  return response.data;
};

export const loginUser = async (
  username: string,
  password: string
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>("/auth/login", {
    username,
    password,
  });

  // Spring Security authorities include the ROLE_ prefix; workspace checks use bare role names.
  const authResponse: AuthResponse = {
    ...response.data,
    roles: response.data.roles.map((role) => role.replace(/^ROLE_/, "")),
  };

  localStorage.setItem("accessToken", authResponse.token);
  localStorage.setItem("user", JSON.stringify(authResponse));

  return authResponse;
};

export const getCurrentUser = async (): Promise<User> => {
  const response = await api.get<User>("/auth/me");
  return response.data;
};

export const logoutUser = (): void => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("user");
};
