# Hosting the Website on Your Own Domain (AWS or DigitalOcean)

This guide helps you move the COPD Fuel website off GitHub Pages to **AWS** or **DigitalOcean** so you host it under **your own domain** and clearly own the site.

## What You're Moving

- **Static site:** The contents of the `docs/` folder:
  - `docs/index.html` – landing page
  - `docs/privacy.html` – privacy policy (linked from the Android app and doctor portal)
- **Your own domain:** e.g. `copdfuel.com` or `www.copdfuel.com` (you must register this at a registrar like Namecheap, Google Domains, or Cloudflare).

After moving, you will:
1. Upload the static files to your chosen host.
2. Point your domain to that host.
3. Update the app and doctor portal to use your new privacy policy URL.

---

## Option A: AWS (S3 + CloudFront)

You host the static files in S3 and serve them through CloudFront. No separate “server” to manage.

### 1. Create an S3 bucket

- In AWS Console: **S3** > **Create bucket**.
- Bucket name: e.g. `copdfuel-website` (must be globally unique).
- Region: e.g. `us-east-1`.
- Uncheck “Block all public access” (CloudFront will access the bucket; you can restrict access to CloudFront only in step 3).
- Create the bucket.

### 2. Enable static website hosting (optional for CloudFront)

- Open the bucket > **Properties** > **Static website hosting** > **Edit**.
- Enable, set index document: `index.html`, error document: `index.html` (for SPA-style fallback if you add more pages later).
- Save.

### 3. Upload files

- In the bucket, create a folder or upload at root: `index.html`, `privacy.html` (same content as in `docs/`).
- Set **Object Ownership** to “ACL disabled” or use bucket policy so that **only CloudFront** can read (recommended). Alternatively, for a quick test, you can make objects public (not ideal for production).

### 4. Create a CloudFront distribution

- **CloudFront** > **Create distribution**.
- **Origin:** Your S3 bucket (e.g. `copdfuel-website.s3.us-east-1.amazonaws.com`). Use the **S3 website endpoint** if you enabled static website hosting, or the **bucket endpoint** otherwise.
- **Default root object:** `index.html`.
- **Alternate domain names (CNAMEs):** e.g. `www.copdfuel.com` and optionally `copdfuel.com`.
- **Custom SSL certificate:** Request or import a certificate in **AWS Certificate Manager (ACM)** in **us-east-1** for `copdfuel.com` and `*.copdfuel.com`, then select it in CloudFront.
- Create the distribution. Note the **distribution domain name** (e.g. `d1234abcd.cloudfront.net`).

### 5. Point your domain to CloudFront

- In your domain registrar (or Route 53):
  - Add a **CNAME** for `www` (or your chosen subdomain) to the CloudFront domain name (e.g. `d1234abcd.cloudfront.net`).
- If you want the root `copdfuel.com` to work, use either:
  - **Route 53** with an **ALIAS** record for the root domain to the CloudFront distribution, or
  - Your registrar’s equivalent (e.g. “flattened” CNAME / ALIAS).

### 6. Your live URLs

- Landing: `https://www.copdfuel.com/` (or `https://www.copdfuel.com/index.html`)
- Privacy: `https://www.copdfuel.com/privacy.html`

---

### Connecting a GoDaddy domain to CloudFront

After your CloudFront distribution is created and working (e.g. `https://d7a355qsg0m82.cloudfront.net`), do the following to use your GoDaddy domain (e.g. `www.copdfuel.com`).

**Part 1: Get an SSL certificate in AWS (required for HTTPS on your domain)**

1. In AWS Console, switch region to **US East (N. Virginia) / us-east-1** (CloudFront only uses certificates from this region).
2. Open **AWS Certificate Manager (ACM)** (search "Certificate Manager" in the top search bar).
3. Click **Request certificate**.
4. Choose **Request a public certificate** > **Next**.
5. **Domain names:** Add:
   - `www.copdfuel.com` (or your subdomain)
   - If you want the root domain too: also add `copdfuel.com`.
6. **Validation method:** **DNS validation** (recommended). Click **Next** > **Request**.
7. On the certificate detail page, open the **Domains** section. For each domain you’ll see a **CNAME name** and **CNAME value**. Leave this tab open; you’ll add these in GoDaddy in Part 3 to validate the certificate.

**Part 2: Add your domain to CloudFront**

1. Go to **CloudFront** > **Distributions** > select **copdfuel-website** (or your distribution).
2. Click **Edit** (top right) or go to the **General** tab and **Edit** under Settings.
3. **Alternate domain names (CNAMEs):** Add `www.copdfuel.com` (and `copdfuel.com` if you validated it). One per line.
4. **Custom SSL certificate:** Choose the certificate you requested in Part 1 from the dropdown.
5. Save changes. Deployment may take a few minutes.

**Part 3: GoDaddy DNS – point your domain to CloudFront**

1. Log in at [godaddy.com](https://www.godaddy.com) > **My Products** > find your domain > **DNS** (or **Manage DNS**).
2. **Add a CNAME record for `www`:**
   - **Type:** CNAME  
   - **Name:** `www` (or `www.copdfuel.com` depending on GoDaddy’s field; some show only the subdomain).  
   - **Value / Points to:** `d7a355qsg0m82.cloudfront.net` (your CloudFront distribution domain – no `https://`, no trailing slash).  
   - **TTL:** 600 or 1 Hour.  
   - Save.
3. **Validate the SSL certificate (ACM):** In the same DNS page, add the **CNAME records that ACM gave you** for each domain (from Part 1). Use the exact **CNAME name** and **CNAME value** from the certificate detail page. These are for validation only; leave them in place.
4. **Root domain (`copdfuel.com` without www):** GoDaddy does not allow a CNAME on the root. Options:
   - **Forwarding:** In GoDaddy, set **Forwarding** so `copdfuel.com` redirects to `https://www.copdfuel.com`. That way everyone who types the root domain lands on www.
   - Or use **AWS Route 53** for DNS (create a hosted zone, add an ALIAS record for the root to CloudFront, then point your domain’s nameservers at Route 53).

Wait 5–30 minutes for DNS to propagate. Then open `https://www.copdfuel.com/` and `https://www.copdfuel.com/privacy.html`. If the certificate is still validating, ACM can take up to 30 minutes; once it shows “Issued,” HTTPS will work.

**Summary**

| Where        | What to do |
|-------------|------------|
| ACM (us-east-1) | Request public cert for `www.copdfuel.com` (and optionally `copdfuel.com`), DNS validation. |
| CloudFront  | Edit distribution: add CNAMEs, select the custom SSL certificate. |
| GoDaddy DNS | CNAME `www` → `d7a355qsg0m82.cloudfront.net`; add ACM validation CNAMEs; use forwarding for root if desired. |

---

## Option B: DigitalOcean

### B1. App Platform (static site) – simplest

- **DigitalOcean** > **App Platform** > **Create App**.
- Choose **Static Site**; connect your GitHub repo (or upload the `docs/` contents).
- Set **Source Directory** to `docs` (or the folder that contains `index.html` and `privacy.html`).
- Under **Resources**, add a **custom domain**: e.g. `www.copdfuel.com`.
- DigitalOcean will give you a CNAME target (e.g. `your-app-xxxxx.ondigitalocean.app`). At your domain registrar, add a CNAME: `www` (or your subdomain) → that target.
- After DNS propagates and SSL is provisioned, your site is at `https://www.copdfuel.com/` and `https://www.copdfuel.com/privacy.html`.

### B2. Spaces + CDN (S3-like)

- Create a **Space** (object storage) in a region.
- Upload `index.html` and `privacy.html` (make them publicly readable or use the Space’s public URL).
- Enable **CDN** on the Space and add your **custom domain** in the Space settings.
- At your registrar, add the CNAME they provide. Your privacy URL will be e.g. `https://www.copdfuel.com/privacy.html`.

---

## After You Go Live: Update the Project

Once the site is live on your domain (e.g. `https://www.copdfuel.com/privacy.html`), update the app and doctor portal so they point to **your** URL.

### 1. Android app

Edit `android/app/build.gradle` and set:

```gradle
buildConfigField "String", "PRIVACY_POLICY_URL", "\"https://www.copdfuel.com/privacy.html\""
```

Replace `https://www.copdfuel.com/privacy.html` with your actual URL. Rebuild the app.

### 2. Doctor portal

Set the env variable and rebuild:

- In `doctor-portal/.env` (or your deployment env):
  - `VITE_PRIVACY_POLICY_URL=https://www.copdfuel.com/privacy.html`

Replace with your actual URL.

### 3. Optional: landing / marketing URL

If you add a landing or marketing URL constant elsewhere in the app or portal, point it to your new domain (e.g. `https://www.copdfuel.com/`).

---

## Claiming Ownership

- **Domain:** Register the domain in **your** name at a registrar (e.g. Namecheap, Cloudflare, Google Domains). The registrar account and domain registration are the legal ownership.
- **Hosting:** With AWS or DigitalOcean, the account (and the S3 bucket / App Platform app / Space) is in your name, so you control the content and the hosting.
- **Content:** The HTML files in `docs/` are part of this repo; once you deploy them to your own hosting and domain, you are publicly serving your own site from your own infrastructure.

---

## Summary

| Step | AWS | DigitalOcean |
|------|-----|--------------|
| Host | S3 + CloudFront | App Platform (static) or Spaces + CDN |
| Domain | Route 53 or any registrar; CNAME/ALIAS to CloudFront | CNAME to App Platform or Space CDN |
| SSL | ACM certificate in us-east-1, attached to CloudFront | Provided by App Platform / Spaces when you add custom domain |
| Update app | `PRIVACY_POLICY_URL` in `android/app/build.gradle` | Same |
| Update portal | `VITE_PRIVACY_POLICY_URL` in doctor-portal env | Same |

After this, the website is on your domain and your AWS or DigitalOcean account, so you can clearly claim ownership of the site.

---

## Later: Storing Patient Data (Do You Need a Server?)

You can host the **website** (landing + privacy) one way and handle **patient data** another. You don’t have to put everything on a single server.

### Keep the website separate

- Keep the **static site** (index + privacy) on **S3 + CloudFront** (or DigitalOcean App Platform / Spaces). No server, low cost, easy to maintain.
- Use **patient data** in a dedicated backend and database, with proper security and (if applicable) HIPAA in mind.

### Where to put patient data

Your Android app already talks to an API at **API Gateway** (`d2gfwfsr2a.execute-api.us-east-2.amazonaws.com`). That API can be extended (or replaced) to read/write patient data. Storage options:

| Approach | Pros | Cons |
|----------|------|------|
| **Managed database (recommended)** | AWS **RDS** (PostgreSQL/MySQL) or **DynamoDB** behind your API (e.g. Lambda or a small API server). Backups, encryption, scaling and often HIPAA-eligible (with AWS BAA). | Slightly more setup than “one box.” |
| **One EC2 server** | One machine: run API (e.g. Node/Spring), database (e.g. PostgreSQL), and nginx for the static site if you want. Simple mental model. | You maintain OS, security, backups, SSL, and (for PHI) hardening and compliance. More ongoing work. |
| **EC2 for API only + managed DB** | API and app logic on EC2; database on **RDS**. You get a “server” for your code but not for the DB. | Good balance: control over the app, less risk for data. |

### Recommendation

- **Website:** Keep it on S3 + CloudFront (or DO static/Spaces). Don’t move it to a server just because you’ll have patient data later.
- **Patient data:** Prefer a **managed database** (RDS or DynamoDB) behind your existing API (or a new API on Lambda/EC2). If you like having a server, use **EC2 for the API only** and put the database on RDS.
- **One server only if:** You explicitly want a single box to learn or to run everything (API + DB + maybe the static site). You’ll need to handle backups, encryption at rest, access control, and (for US healthcare data) HIPAA considerations and a BAA with AWS.

**HIPAA note:** Patient data is PHI. Use encryption (in transit and at rest), strict access controls, and a **Business Associate Agreement (BAA)** with your cloud provider where required. AWS offers a BAA for many services (e.g. RDS, S3, Lambda) when you enable and configure them in a HIPAA-eligible way.
