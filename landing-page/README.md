# FitPulse landing page — deploy bundle

Upload the contents of this folder to any static host (Netlify, Vercel, Cloudflare Pages, GitHub Pages, S3, or plain nginx/Apache). No build step, no server code.

## Files

- `index.html` — the landing page
- `support.js` — runtime the page loads (must sit next to `index.html`)
- `favicon.png` — site icon
- `fitpulse-*-screenshot.png` — phone screenshots used in the hero and the app tour
- `detail-*.png` — the 12 feature gallery cards
- `privacy/index.html` — privacy policy, served at `/privacy`

## Notes

- The page loads React and Google Fonts from public CDNs at runtime, so visitors need internet access (true of any hosted site).
- The Play Store link points to `play.google.com/store/apps/details?id=ph.mart.healthapp`. Search `index.html` for that URL to change it.
- Update `og:image` in `index.html` to a full absolute URL (e.g. `https://yourdomain.com/fitpulse-home-screenshot.png`) once you know the domain — social previews need an absolute path.
- The footer has no privacy link yet; add one pointing to `/privacy` if you want it reachable from the page.
