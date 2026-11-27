# Library Hub front end

The React 19 + Vite front end of the Library Reservation & Review Hub. The [main README](../README.md) describes the
app and how to run all of it; [docs/DESIGN.md](../docs/DESIGN.md) describes how it is built.

```bash
npm install
npm run dev      # http://localhost:5173, with /api proxied to the back end on :8432
npm test         # Vitest unit and component tests
npm run lint     # ESLint
npm run build    # type check and production build into dist/
```

- `src/services/`: the one axios client (`api.js`, which handles ended sessions) and a module per area.
- `src/components/`: pages; `dashboard/`, `book/` and `admin/` hold the parts of the three largest.
- `src/hooks/`: the book list, the theme and the AI chat.
