export const passwordRequirements = (senha: string) => [
  {
    label: "10 a 72 caracteres",
    valid: senha.length >= 10 && senha.length <= 72,
  },
  { label: "letra maiúscula", valid: /\p{Lu}/u.test(senha) },
  { label: "letra minúscula", valid: /\p{Ll}/u.test(senha) },
  { label: "número", valid: /\p{Nd}/u.test(senha) },
  { label: "caractere especial", valid: /[^\p{L}\p{N}\s]/u.test(senha) },
];
