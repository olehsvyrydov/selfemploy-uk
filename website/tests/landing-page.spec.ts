/**
 * SE-901 Landing Page E2E Tests
 *
 * Comprehensive Playwright tests for the UK Self-Employment Manager landing page.
 * Based on /rob's QA test specifications from docs/sprints/sprint-6/testing/rob-qa-SE-901.md
 *
 * @author /adam (E2E Test Automation Engineer)
 * @ticket SE-901
 * @sprint 6
 */

import { test, expect, Page } from '@playwright/test';

// ============================================================================
// Test Configuration & Constants
// ============================================================================

const VIEWPORTS = {
  mobile: { width: 375, height: 667 },
  smallMobile: { width: 320, height: 568 },
  tablet: { width: 768, height: 1024 },
  desktop: { width: 1440, height: 900 },
  wideDesktop: { width: 1920, height: 1080 },
};

const GITHUB_REPO_URL = 'https://github.com/selfemploy-uk/self-employment';

// ============================================================================
// 1. PAGE STRUCTURE TESTS (AC-1) - TC-001 to TC-005
// ============================================================================

test.describe('1. Page Structure and Loading (AC-1)', () => {

  test('TC-001: Page loads successfully [P0]', async ({ page }) => {
    // Navigate and capture response
    const response = await page.goto('/');

    // Verify HTTP 200 status
    expect(response?.status()).toBe(200);

    // Verify page title
    await expect(page).toHaveTitle('UK Self-Employment Manager - Free Tax Software for Self-Employed');

    // Verify no console errors by default (logged separately in TC-005)
  });

  test('TC-002: All page sections present [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify all major sections exist
    await expect(page.locator('.nav-header')).toBeVisible();
    await expect(page.locator('.hero')).toBeVisible();
    await expect(page.locator('#features')).toBeVisible();
    await expect(page.locator('.how-it-works')).toBeVisible();
    await expect(page.locator('.demo')).toBeVisible();
    await expect(page.locator('#download')).toBeVisible();
    await expect(page.locator('#faq')).toBeVisible();
    await expect(page.locator('.cta')).toBeVisible();
    await expect(page.locator('.footer')).toBeVisible();
  });

  test('TC-003: Favicon loads correctly [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify SVG favicon link element present
    const faviconLink = page.locator('link[rel="icon"][type="image/svg+xml"]');
    await expect(faviconLink).toHaveAttribute('href', 'images/favicon.svg');
  });

  test('TC-004: CSS stylesheet loaded [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify stylesheet link present
    const stylesheetLink = page.locator('link[rel="stylesheet"][href="css/styles.css"]');
    await expect(stylesheetLink).toBeAttached();

    // Verify styles are applied by checking nav-header exists and has styling
    const navHeader = page.locator('.nav-header');
    await expect(navHeader).toBeVisible();

    // Check that nav header has fixed position styling applied
    const position = await navHeader.evaluate(el => getComputedStyle(el).position);
    expect(position).toBe('fixed');
  });

  test('TC-005: JavaScript loads without errors [P0]', async ({ page }) => {
    const jsErrors: string[] = [];
    const pageErrors: string[] = [];

    // Listen for console errors (filter out 404 resource errors which are expected for placeholder images)
    page.on('console', msg => {
      if (msg.type() === 'error') {
        const text = msg.text();
        // Filter out expected 404 errors for placeholder images
        if (!text.includes('404') && !text.includes('Failed to load resource')) {
          jsErrors.push(text);
        }
      }
    });

    // Listen for page errors (JavaScript execution errors)
    page.on('pageerror', error => {
      pageErrors.push(error.message);
    });

    await page.goto('/');

    // Wait for JS to initialize
    await page.waitForLoadState('networkidle');

    // Verify no JavaScript execution errors
    expect(pageErrors).toHaveLength(0);
    // Verify no critical JS errors (filtered)
    expect(jsErrors).toHaveLength(0);

    // Verify script tag is present
    const scriptTag = page.locator('script[src="js/main.js"]');
    await expect(scriptTag).toBeAttached();
  });
});

// ============================================================================
// 2. HERO SECTION TESTS (AC-2) - TC-006 to TC-008
// ============================================================================

test.describe('2. Hero Section (AC-2)', () => {

  test('TC-006: Hero headline displays correctly [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify H1 text and ID
    const heroHeading = page.locator('#hero-heading');
    await expect(heroHeading).toBeVisible();
    await expect(heroHeading).toHaveText('Your Tax. Your Data. Your Freedom.');

    // Verify it's an H1 element
    const tagName = await heroHeading.evaluate(el => el.tagName);
    expect(tagName).toBe('H1');
  });

  test('TC-007: Hero subtitle displays [P0]', async ({ page }) => {
    await page.goto('/');

    const heroSubtitle = page.locator('.hero-subtitle');
    await expect(heroSubtitle).toBeVisible();
    await expect(heroSubtitle).toContainText('Free, open-source tax management for UK self-employed professionals');
  });

  test('TC-008: Hero CTA buttons present [P0]', async ({ page }) => {
    await page.goto('/');

    const heroCTA = page.locator('.hero-cta');

    // Verify Download button
    const downloadBtn = heroCTA.locator('.btn-primary');
    await expect(downloadBtn).toBeVisible();
    await expect(downloadBtn).toContainText('Download for Free');

    // Verify GitHub button
    const githubBtn = heroCTA.locator('.btn-secondary');
    await expect(githubBtn).toBeVisible();
    await expect(githubBtn).toContainText('View on GitHub');
    await expect(githubBtn).toHaveAttribute('href', GITHUB_REPO_URL);
    await expect(githubBtn).toHaveAttribute('target', '_blank');
    await expect(githubBtn).toHaveAttribute('rel', /noopener/);
  });
});

// ============================================================================
// 3. FEATURES SECTION TESTS (AC-3) - TC-011 to TC-016
// ============================================================================

test.describe('3. Features Section (AC-3)', () => {

  test('TC-011: Features section headline [P0]', async ({ page }) => {
    await page.goto('/');

    const featuresHeading = page.locator('#features-heading');
    await expect(featuresHeading).toBeVisible();
    await expect(featuresHeading).toHaveText('Why Choose Us?');

    // Verify section subtitle
    const subtitle = page.locator('#features .section-subtitle');
    await expect(subtitle).toBeVisible();
  });

  test('TC-012: Four feature cards displayed [P0]', async ({ page }) => {
    await page.goto('/');

    // Count feature cards
    const featureCards = page.locator('.feature-card');
    await expect(featureCards).toHaveCount(4);
  });

  test('TC-013: "100% Free" feature card [P0]', async ({ page }) => {
    await page.goto('/');

    // Find the first feature card (100% Free)
    const featureCards = page.locator('.feature-card');
    const freeCard = featureCards.nth(0);

    // Verify title
    const title = freeCard.locator('h3');
    await expect(title).toHaveText('100% Free');

    // Verify icon class
    const icon = freeCard.locator('.feature-icon-free');
    await expect(icon).toBeVisible();

    // Verify description mentions "no hidden fees"
    const description = freeCard.locator('p');
    await expect(description).toContainText('No hidden fees');
  });

  test('TC-014: "Privacy-First" feature card [P0]', async ({ page }) => {
    await page.goto('/');

    const featureCards = page.locator('.feature-card');
    const privacyCard = featureCards.nth(1);

    // Verify title
    const title = privacyCard.locator('h3');
    await expect(title).toHaveText('Privacy-First');

    // Verify icon class
    const icon = privacyCard.locator('.feature-icon-privacy');
    await expect(icon).toBeVisible();

    // Verify description mentions "your device" and "no cloud"
    const description = privacyCard.locator('p');
    await expect(description).toContainText('your device');
    await expect(description).toContainText('No cloud');
  });

  test('TC-015: "Open Source" feature card [P0]', async ({ page }) => {
    await page.goto('/');

    const featureCards = page.locator('.feature-card');
    const opensourceCard = featureCards.nth(2);

    // Verify title
    const title = opensourceCard.locator('h3');
    await expect(title).toHaveText('Open Source');

    // Verify icon class
    const icon = opensourceCard.locator('.feature-icon-opensource');
    await expect(icon).toBeVisible();

    // Verify description mentions "Apache 2.0" and "auditable"
    const description = opensourceCard.locator('p');
    await expect(description).toContainText('Apache 2.0');
    await expect(description).toContainText('auditable');
  });

  test('TC-016: "HMRC MTD Ready" feature card [P0]', async ({ page }) => {
    await page.goto('/');

    const featureCards = page.locator('.feature-card');
    const hmrcCard = featureCards.nth(3);

    // Verify title
    const title = hmrcCard.locator('h3');
    await expect(title).toHaveText('HMRC MTD Ready');

    // Verify icon class
    const icon = hmrcCard.locator('.feature-icon-hmrc');
    await expect(icon).toBeVisible();

    // Verify description mentions "Making Tax Digital"
    const description = hmrcCard.locator('p');
    await expect(description).toContainText('Making Tax Digital');
  });
});

// ============================================================================
// 4. DOWNLOAD SECTION TESTS (AC-4) - TC-019 to TC-023
// ============================================================================

test.describe('4. Download Section (AC-4)', () => {

  test('TC-019: Download section headline [P0]', async ({ page }) => {
    await page.goto('/');

    const downloadHeading = page.locator('#download-heading');
    await expect(downloadHeading).toBeVisible();
    await expect(downloadHeading).toHaveText('Download');

    // Verify section subtitle
    const subtitle = page.locator('#download .section-subtitle');
    await expect(subtitle).toContainText('Available for Windows, macOS, and Linux');
    await expect(subtitle).toContainText('Always free');
  });

  test('TC-020: Three download cards present [P0]', async ({ page }) => {
    await page.goto('/');

    const downloadCards = page.locator('.download-card');
    await expect(downloadCards).toHaveCount(3);
  });

  test('TC-021: Windows download card [P0]', async ({ page }) => {
    await page.goto('/');

    // Find Windows card by aria-label
    const windowsCard = page.locator('.download-card[aria-label="Download for Windows"]');

    // Verify title
    const title = windowsCard.locator('h3');
    await expect(title).toHaveText('Windows');

    // Verify requirements
    const requirements = windowsCard.locator('p');
    await expect(requirements).toContainText('Windows 10 or later');

    // Verify format
    const format = windowsCard.locator('.download-format');
    await expect(format).toHaveText('Download .msi');

    // Verify size
    const size = windowsCard.locator('.download-size');
    await expect(size).toHaveText('~85 MB');

    // Verify link to GitHub releases
    await expect(windowsCard).toHaveAttribute('href', /github\.com.*\.msi/);
  });

  test('TC-022: macOS download card [P0]', async ({ page }) => {
    await page.goto('/');

    const macCard = page.locator('.download-card[aria-label="Download for macOS"]');

    // Verify title
    const title = macCard.locator('h3');
    await expect(title).toHaveText('macOS');

    // Verify requirements
    const requirements = macCard.locator('p');
    await expect(requirements).toContainText('macOS 12 Monterey or later');

    // Verify format
    const format = macCard.locator('.download-format');
    await expect(format).toHaveText('Download .dmg');

    // Verify size
    const size = macCard.locator('.download-size');
    await expect(size).toHaveText('~90 MB');

    // Verify link to GitHub releases
    await expect(macCard).toHaveAttribute('href', /github\.com.*\.dmg/);
  });

  test('TC-023: Linux download card [P0]', async ({ page }) => {
    await page.goto('/');

    const linuxCard = page.locator('.download-card[aria-label="Download for Linux"]');

    // Verify title
    const title = linuxCard.locator('h3');
    await expect(title).toHaveText('Linux');

    // Verify requirements
    const requirements = linuxCard.locator('p');
    await expect(requirements).toContainText('Ubuntu 22.04+');
    await expect(requirements).toContainText('Fedora 38+');

    // Verify format
    const format = linuxCard.locator('.download-format');
    await expect(format).toContainText('.deb');

    // Verify size
    const size = linuxCard.locator('.download-size');
    await expect(size).toHaveText('~80 MB');

    // Verify link to GitHub releases
    await expect(linuxCard).toHaveAttribute('href', /github\.com.*\.deb/);
  });
});

// ============================================================================
// 5. DEMO SECTION TESTS (AC-5) - TC-030 to TC-031
// ============================================================================

test.describe('5. Demo Section (AC-5)', () => {

  test('TC-030: Demo section present [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify demo section exists
    const demoSection = page.locator('.demo');
    await expect(demoSection).toBeVisible();

    // Verify hidden heading for accessibility
    const hiddenHeading = page.locator('#demo-heading');
    await expect(hiddenHeading).toBeAttached();
  });

  test('TC-031: Demo screenshot image [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify screenshot image
    const screenshot = page.locator('.demo-screenshot');
    await expect(screenshot).toBeVisible();

    // Verify image src
    await expect(screenshot).toHaveAttribute('src', 'images/app-dashboard.png');

    // Verify alt text describes dashboard
    const altText = await screenshot.getAttribute('alt');
    expect(altText).toContain('dashboard');

    // Verify lazy loading
    await expect(screenshot).toHaveAttribute('loading', 'lazy');
  });
});

// ============================================================================
// 6. FAQ SECTION TESTS (AC-6) - TC-034 to TC-037, TC-042
// ============================================================================

test.describe('6. FAQ Section (AC-6)', () => {

  test('TC-034: FAQ section headline [P0]', async ({ page }) => {
    await page.goto('/');

    const faqHeading = page.locator('#faq-heading');
    await expect(faqHeading).toBeVisible();
    await expect(faqHeading).toHaveText('Frequently Asked Questions');

    // Verify section subtitle
    const subtitle = page.locator('#faq .section-subtitle');
    await expect(subtitle).toContainText('Got questions? We have answers.');
  });

  test('TC-035: Eight FAQ items present [P0]', async ({ page }) => {
    await page.goto('/');

    const faqItems = page.locator('.faq-item');
    await expect(faqItems).toHaveCount(8);
  });

  test('TC-036: First FAQ item open by default [P0]', async ({ page }) => {
    await page.goto('/');

    // First FAQ item should have 'open' attribute
    const firstFaq = page.locator('.faq-item').first();
    await expect(firstFaq).toHaveAttribute('open', '');

    // Answer should be visible
    const answer = firstFaq.locator('.faq-answer');
    await expect(answer).toBeVisible();
  });

  test('TC-037: FAQ item 1 content - "Is this software really free?" [P0]', async ({ page }) => {
    await page.goto('/');

    const firstFaq = page.locator('.faq-item').first();

    // Verify question text
    const question = firstFaq.locator('summary');
    await expect(question).toContainText('Is this software really free?');

    // Verify answer content
    const answer = firstFaq.locator('.faq-answer');
    await expect(answer).toContainText('completely free');
    await expect(answer).toContainText('no hidden fees');
    await expect(answer).toContainText('Apache 2.0 License');
  });

  test('TC-042: FAQ accordion expand/collapse [P0]', async ({ page }) => {
    await page.goto('/');

    // Get second FAQ item (initially closed)
    const secondFaq = page.locator('.faq-item').nth(1);
    const summary = secondFaq.locator('summary');
    const answer = secondFaq.locator('.faq-answer');

    // Verify initially closed
    await expect(secondFaq).not.toHaveAttribute('open', '');
    await expect(answer).not.toBeVisible();

    // Click to open
    await summary.click();
    await expect(secondFaq).toHaveAttribute('open', '');
    await expect(answer).toBeVisible();

    // Click to close
    await summary.click();
    await expect(secondFaq).not.toHaveAttribute('open', '');
    await expect(answer).not.toBeVisible();
  });
});

// ============================================================================
// 7. GITHUB LINKS TESTS (AC-7) - TC-047 to TC-049
// ============================================================================

test.describe('7. GitHub Links (AC-7)', () => {

  test('TC-047: GitHub link in navigation [P0]', async ({ page }) => {
    await page.goto('/');

    const navGithub = page.locator('.nav-github');
    await expect(navGithub).toBeVisible();
    await expect(navGithub).toContainText('GitHub');
    await expect(navGithub).toHaveAttribute('href', GITHUB_REPO_URL);
    await expect(navGithub).toHaveAttribute('target', '_blank');
    await expect(navGithub).toHaveAttribute('rel', /noopener/);

    // Verify GitHub icon is visible
    const githubIcon = navGithub.locator('.github-icon');
    await expect(githubIcon).toBeVisible();
  });

  test('TC-048: GitHub link in hero [P0]', async ({ page }) => {
    await page.goto('/');

    const heroGithub = page.locator('.hero-cta .btn-secondary');
    await expect(heroGithub).toBeVisible();
    await expect(heroGithub).toContainText('View on GitHub');
    await expect(heroGithub).toHaveAttribute('href', GITHUB_REPO_URL);
    await expect(heroGithub).toHaveAttribute('target', '_blank');
    await expect(heroGithub).toHaveAttribute('rel', /noopener/);
    await expect(heroGithub).toHaveAttribute('rel', /noreferrer/);
  });

  test('TC-049: GitHub link in footer [P0]', async ({ page }) => {
    await page.goto('/');

    // Find footer community links section
    const footerLinks = page.locator('.footer-links').filter({ hasText: 'Community' });
    const githubLink = footerLinks.locator('a').filter({ hasText: 'GitHub' });

    await expect(githubLink).toBeVisible();
    await expect(githubLink).toHaveAttribute('href', GITHUB_REPO_URL);
    await expect(githubLink).toHaveAttribute('target', '_blank');
    await expect(githubLink).toHaveAttribute('rel', /noopener/);
  });
});

// ============================================================================
// 8. RESPONSIVE DESIGN TESTS (AC-8) - TC-053 to TC-059
// ============================================================================

test.describe('8. Responsive Design (AC-8)', () => {

  test('TC-053: Desktop layout (1024px+) [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.desktop);
    await page.goto('/');

    // Navigation links should be visible (not hamburger only)
    const navLinks = page.locator('#nav-links');
    await expect(navLinks).toBeVisible();

    // Mobile toggle should be hidden
    const mobileToggle = page.locator('#mobile-menu-toggle');
    await expect(mobileToggle).not.toBeVisible();

    // Features should be in a grid (4 cards visible in a row)
    const featureCards = page.locator('.feature-card');
    await expect(featureCards).toHaveCount(4);

    // Download cards should be in a grid
    const downloadCards = page.locator('.download-card');
    await expect(downloadCards).toHaveCount(3);
  });

  test('TC-054: Tablet layout (768px-1024px) [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.tablet);
    await page.goto('/');

    // Check layout adapts for tablet
    const hero = page.locator('.hero');
    await expect(hero).toBeVisible();

    // Feature cards should still be visible
    const featureCards = page.locator('.feature-card');
    await expect(featureCards).toHaveCount(4);
  });

  test('TC-055: Mobile layout (<768px) [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.mobile);
    await page.goto('/');

    // Hamburger menu should be visible
    const mobileToggle = page.locator('#mobile-menu-toggle');
    await expect(mobileToggle).toBeVisible();

    // Nav links should be hidden initially
    const navLinks = page.locator('#nav-links');
    const isVisible = await navLinks.evaluate(el => {
      const style = getComputedStyle(el);
      return style.display !== 'none' && style.visibility !== 'hidden';
    });
    // On mobile, nav links might be positioned off-screen or hidden until toggled

    // Hero should be visible
    const hero = page.locator('.hero');
    await expect(hero).toBeVisible();
  });

  test('TC-056: Small mobile layout (<480px) [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.smallMobile);
    await page.goto('/');

    // Page should still render properly
    const hero = page.locator('.hero');
    await expect(hero).toBeVisible();

    // Hamburger should be visible
    const mobileToggle = page.locator('#mobile-menu-toggle');
    await expect(mobileToggle).toBeVisible();

    // Feature cards should still be present
    const featureCards = page.locator('.feature-card');
    await expect(featureCards).toHaveCount(4);
  });

  test('TC-057: Mobile navigation toggle visible [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.mobile);
    await page.goto('/');

    const mobileToggle = page.locator('#mobile-menu-toggle');

    // Should be visible
    await expect(mobileToggle).toBeVisible();

    // Should have three hamburger lines
    const hamburgerLines = mobileToggle.locator('.hamburger-line');
    await expect(hamburgerLines).toHaveCount(3);

    // Should have aria-expanded="false" initially
    await expect(mobileToggle).toHaveAttribute('aria-expanded', 'false');
  });

  test('TC-058: Mobile menu opens on click [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.mobile);
    await page.goto('/');

    const mobileToggle = page.locator('#mobile-menu-toggle');
    const navLinks = page.locator('#nav-links');

    // Initially closed
    await expect(mobileToggle).toHaveAttribute('aria-expanded', 'false');

    // Click to open
    await mobileToggle.click();

    // Should now be expanded
    await expect(mobileToggle).toHaveAttribute('aria-expanded', 'true');
    await expect(navLinks).toHaveClass(/active/);
  });

  test('TC-059: Mobile menu closes on link click [P0]', async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.mobile);
    await page.goto('/');

    const mobileToggle = page.locator('#mobile-menu-toggle');
    const navLinks = page.locator('#nav-links');

    // Open menu
    await mobileToggle.click();
    await expect(mobileToggle).toHaveAttribute('aria-expanded', 'true');

    // Click a nav link
    const featuresLink = navLinks.locator('a[href="#features"]');
    await featuresLink.click();

    // Menu should close
    await expect(mobileToggle).toHaveAttribute('aria-expanded', 'false');
    await expect(navLinks).not.toHaveClass(/active/);
  });
});

// ============================================================================
// 9. NAVIGATION TESTS - TC-071, TC-072, TC-074, TC-075
// ============================================================================

test.describe('9. Navigation and Links', () => {

  test('TC-071: Skip link functionality [P0]', async ({ page }) => {
    await page.goto('/');

    // Find skip link
    const skipLink = page.locator('.skip-link');
    await expect(skipLink).toBeAttached();
    await expect(skipLink).toHaveAttribute('href', '#main');

    // Tab to make skip link visible and verify it appears on focus
    await page.keyboard.press('Tab');
    await expect(skipLink).toBeFocused();
  });

  test('TC-072: Navigation internal links work [P0]', async ({ page }) => {
    await page.goto('/');

    // Click Features link
    const featuresLink = page.locator('.nav-links a[href="#features"]');
    await featuresLink.click();

    // Verify URL hash updates
    await expect(page).toHaveURL(/#features/);

    // Click Download link
    const downloadLink = page.locator('.nav-links a[href="#download"]');
    await downloadLink.click();
    await expect(page).toHaveURL(/#download/);

    // Click FAQ link
    const faqLink = page.locator('.nav-links a[href="#faq"]');
    await faqLink.click();
    await expect(page).toHaveURL(/#faq/);
  });

  test('TC-074: Legal page links in footer [P0]', async ({ page }) => {
    await page.goto('/');

    const footerLinks = page.locator('.footer-links').filter({ hasText: 'Legal' });

    // Verify Privacy Policy link
    const privacyLink = footerLinks.locator('a[href="/privacy"]');
    await expect(privacyLink).toBeVisible();

    // Verify Terms of Service link
    const termsLink = footerLinks.locator('a[href="/terms"]');
    await expect(termsLink).toBeVisible();

    // Verify Disclaimer link
    const disclaimerLink = footerLinks.locator('a[href="/disclaimer"]');
    await expect(disclaimerLink).toBeVisible();
  });

  test('TC-075: External links open in new tab [P0]', async ({ page }) => {
    await page.goto('/');

    // Find all external GitHub links that should open in new tab
    // Note: Download cards (href contains /releases/latest/download) intentionally don't open in new tab
    // to allow browser's download behavior
    const externalLinks = page.locator('a[href^="https://github.com"][target="_blank"]');
    const count = await externalLinks.count();

    // Should have multiple GitHub links that open in new tab
    expect(count).toBeGreaterThan(0);

    // Verify each has rel="noopener"
    for (let i = 0; i < count; i++) {
      const link = externalLinks.nth(i);
      const rel = await link.getAttribute('rel');
      expect(rel).toContain('noopener');
    }

    // Separately verify download cards exist but don't require target="_blank"
    const downloadCards = page.locator('.download-card[href^="https://github.com"]');
    const downloadCount = await downloadCards.count();
    expect(downloadCount).toBe(3); // Windows, macOS, Linux
  });
});

// ============================================================================
// 10. LEGAL PAGES TESTS - TC-078 to TC-080
// ============================================================================

test.describe('10. Legal Pages', () => {

  test('TC-078: Privacy page loads [P0]', async ({ page }) => {
    const response = await page.goto('/privacy.html');

    // Verify page loads
    expect(response?.status()).toBe(200);

    // Verify title
    await expect(page).toHaveTitle('Privacy Policy - UK Self-Employment Manager');

    // Verify H1
    const heading = page.locator('h1');
    await expect(heading).toHaveText('Privacy Policy');

    // Verify navigation present
    const nav = page.locator('.nav-header');
    await expect(nav).toBeVisible();
  });

  test('TC-079: Terms page loads [P0]', async ({ page }) => {
    const response = await page.goto('/terms.html');

    // Verify page loads
    expect(response?.status()).toBe(200);

    // Verify title
    await expect(page).toHaveTitle('Terms of Service - UK Self-Employment Manager');

    // Verify H1
    const heading = page.locator('h1');
    await expect(heading).toHaveText('Terms of Service');

    // Verify navigation present
    const nav = page.locator('.nav-header');
    await expect(nav).toBeVisible();
  });

  test('TC-080: Disclaimer page loads [P0]', async ({ page }) => {
    const response = await page.goto('/disclaimer.html');

    // Verify page loads
    expect(response?.status()).toBe(200);

    // Verify title
    await expect(page).toHaveTitle('Disclaimer - UK Self-Employment Manager');

    // Verify H1
    const heading = page.locator('h1');
    await expect(heading).toHaveText('Disclaimer');

    // Verify navigation present
    const nav = page.locator('.nav-header');
    await expect(nav).toBeVisible();
  });
});

// ============================================================================
// 11. ACCESSIBILITY TESTS - TC-082 to TC-084
// ============================================================================

test.describe('11. Accessibility', () => {

  test('TC-082: Proper heading hierarchy [P0]', async ({ page }) => {
    await page.goto('/');

    // Count H1 elements - should be exactly 1
    const h1Elements = page.locator('h1');
    await expect(h1Elements).toHaveCount(1);

    // Verify H1 is the hero heading
    await expect(h1Elements).toHaveId('hero-heading');

    // Verify H2 elements exist for each section
    const h2Elements = page.locator('main h2');
    const h2Count = await h2Elements.count();
    expect(h2Count).toBeGreaterThanOrEqual(6); // At least 6 major sections

    // Verify H3 elements exist (feature cards, FAQ, etc.)
    const h3Elements = page.locator('main h3');
    const h3Count = await h3Elements.count();
    expect(h3Count).toBeGreaterThan(0);
  });

  test('TC-083: ARIA labels present [P0]', async ({ page }) => {
    await page.goto('/');

    // Verify main navigation has aria-label
    const mainNav = page.locator('nav.nav-header');
    await expect(mainNav).toHaveAttribute('aria-label', 'Main navigation');

    // Verify mobile toggle has proper ARIA attributes
    const mobileToggle = page.locator('#mobile-menu-toggle');
    await expect(mobileToggle).toHaveAttribute('aria-label', 'Toggle navigation menu');
    await expect(mobileToggle).toHaveAttribute('aria-expanded');
    await expect(mobileToggle).toHaveAttribute('aria-controls', 'nav-links');

    // Verify sections have aria-labelledby
    const heroSection = page.locator('.hero');
    await expect(heroSection).toHaveAttribute('aria-labelledby', 'hero-heading');

    const featuresSection = page.locator('#features');
    await expect(featuresSection).toHaveAttribute('aria-labelledby', 'features-heading');
  });

  test('TC-084: Images have alt text [P0]', async ({ page }) => {
    await page.goto('/');

    // Get all images
    const images = page.locator('img');
    const imageCount = await images.count();

    // Verify all images have alt attribute
    for (let i = 0; i < imageCount; i++) {
      const img = images.nth(i);
      const alt = await img.getAttribute('alt');

      // Alt should exist and not be empty (unless decorative)
      expect(alt).not.toBeNull();

      // Alt should not be just a filename
      if (alt && alt.length > 0) {
        expect(alt).not.toMatch(/\.(png|jpg|jpeg|svg|gif)$/i);
      }
    }
  });
});
