---
name: 'react19-typescript-mui'
description: 'Enforces concise coding standards for React 19, TypeScript 5+, React Router 7 Layouts, and Material UI 6.'
version: '1.1.0'
frameworks:
  - 'react@19.x'
  - 'typescript@5.x'
  - 'react-router@7.x'
  - '@mui/material@6.x'
---

# React 19, TypeScript & MUI Code Generation Blueprint

Use this skill whenever generating, editing, or refactoring frontend user interfaces, routers, forms, or view-layer logic.

## 1. Architectural Guardrails (React 19 & Router 7)

- **Form Management (Native Actions):** Do not use legacy `onSubmit={(e) => e.preventDefault()}` event patterns. Always use React 19's native `<form action={actionFunction}>`.
- **Form State Tracking:** Use the native `useActionState` hook to automatically manage loading states, return error matrices from the backend, and track pending submissions. Avoid manual `const [loading, setLoading] = useState(false)` state hooks.
- **Ref Handling:** Never use the deprecated `forwardRef` API. React 19 passes `ref` as a standard prop. Accept it transparently: `function CustomInput({ ref, ...props })`.
- **Layout Routing:** Adhere strictly to the layout routing model. Global wrappers, structural headers, side navigation, and base view containers belong in layout components using `<Outlet />`. Individual page components must be clean fragments.

## 2. Global Styling & Layout Strategy (MUI 6)

- **Layout Separation:** Individual page views must never write root-level `<Container>`, `<main>`, or full-screen layout tags. They start directly with typography headers and layout-agnostic content components.
- **Styling Utility:** Banned: External CSS modules, raw `style={...}` tags, and custom styled-components strings. Authorized: The native MUI `sx` utility property, driving layout via design system token integers (e.g., `sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}`).
- **Data Collections:** Use standard out-of-the-box MUI `<Table>` or `<List>` elements. Keep data tables clean without inventing custom grid layout architectures.

## 3. TypeScript Specifications

- **Component Typing:** Component properties must be typed explicitly using a distinct `interface` block positioned directly above the component declaration. Avoid inline typing objects and strictly forbid `any`.
- **Backend Type Synchronization:** Every incoming payload from the Java Spring Boot backend (like DTO responses) must map to an identical TypeScript `interface` file tracking structure and validation boundaries.

## 4. Production Code Blueprints

### Preferred Architecture: Layout Routing Setup
```tsx
import { Outlet } from 'react-router';
import { Box, Container, AppBar, Toolbar, Typography } from '@mui/material';

// The centralized frame. Pages inherit this automatically.
export function MainLayout() {
  return (
    <Box '100vh' 'column', 'flex', display: flexDirection: minHeight: sx="{{" }}>
      <AppBar position="static">
        <Toolbar><Typography variant="h6">Application</Typography></Toolbar>
      </AppBar>
      <Container 1, 4 component="main" flexGrow: maxWidth="lg" py: sx="{{" }}>
        <Outlet/> 
      </Container>
    </Box>
  );
}