import puppeteer from 'puppeteer';

const FRONTEND_URL = 'http://localhost:5173';
const BOT_NAMES = ['Billy', 'Rose', 'Lucky', 'Jesse', 'Bart', 'Sid'];

async function main() {
  console.log('🤠 BANG! Automated Game Launcher (Multi-Tab) 🤠');
  console.log('Launching browser...');

  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: ['--start-maximized', '--no-sandbox', '--disable-setuid-sandbox'],
    dumpio: true // useful for debugging
  });

  try {
    const pages = [];

    // --- PLAYER 1 (HOST) ---
    console.log('Creating Host (Player 1)...');
    const page1 = await browser.newPage();
    pages.push(page1);
    await page1.goto(FRONTEND_URL);

    // Enter Name
    const nameInputSelector = 'input[placeholder="Enter your cowboy name..."]';
    await page1.waitForSelector(nameInputSelector);
    await page1.type(nameInputSelector, 'Player 1');

    // Create Room
    // Using class selector which is more robust than text
    const createButtonSelector = 'button.btn-western'; // The first btn-western is Create Room? Let's verify context.
    // Actually, "Create Room" button is inside the div with "Create a Room". 
    // We can iterate or find by text using evaluate if ::-p-text is flaky.
    // But let's try ::-p-text again, it usually works in recent Puppeteer.
    const createBtnSelector = 'button ::-p-text("Create Room")';
    await page1.waitForSelector(createBtnSelector);
    await page1.click(createBtnSelector);

    // Wait for Lobby and Room Code
    console.log('Waiting for lobby...');
    await page1.waitForSelector('.font-mono', { timeout: 10000 });
    
    const roomCodeElement = await page1.$('.font-mono');
    const roomCode = await page1.evaluate(el => el.textContent.trim(), roomCodeElement);
    console.log(`\n🎉 Room Created! Code: ${roomCode}\n`);


    // --- BOTS (SEPARATE TABS) ---
    console.log('Launching Bot Tabs...');
    
    for (const name of BOT_NAMES) {
      console.log(`Opening tab for ${name}...`);
      const page = await browser.newPage();
      pages.push(page);
      
      // Use Auto-Join URL feature in Home.vue
      const joinUrl = `${FRONTEND_URL}/?autoJoin=true&roomCode=${roomCode}&playerName=${name}`;
      await page.goto(joinUrl);
      
      // Wait for them to land in lobby
      await page.waitForSelector('.font-mono', { timeout: 10000 });
      console.log(`[${name}] Joined lobby!`);
    }

    console.log('All players joined. Readying up...');

    // --- READY UP EVERYONE ---
    // We iterate through all pages (including Host)
    for (let i = 0; i < pages.length; i++) {
        const page = pages[i];
        const playerLabel = i === 0 ? 'Player 1' : BOT_NAMES[i-1];
        
        // Bring tab to front
        await page.bringToFront();

        // Wait for "Ready Up" button
        try {
            const readyBtnSelector = 'button ::-p-text("Ready Up")';
            await page.waitForSelector(readyBtnSelector, { timeout: 5000 });
            await page.click(readyBtnSelector);
            console.log(`[${playerLabel}] Readied up.`);
            // Short delay to see the action
            await new Promise(r => setTimeout(r, 500));
        } catch (e) {
            console.warn(`[${playerLabel}] Could not click Ready Up (maybe already ready?):`, e.message);
        }
    }

    // --- START GAME (HOST ONLY) ---
    await page1.bringToFront();
    console.log('Waiting for "Start Game" button on Host...');
    
    // Wait for the Start Game button to be enabled
    await page1.waitForFunction(
        () => {
            const btns = Array.from(document.querySelectorAll('button'));
            const startBtn = btns.find(b => b.textContent.includes('Start Game'));
            return startBtn && !startBtn.disabled;
        },
        { timeout: 10000 }
    );

    console.log('Starting the game! 🔫');
    const startBtnSelector = 'button ::-p-text("Start Game")';
    await page1.click(startBtnSelector);

    console.log('\n✅ Game started! You have 4 tabs open.');
    console.log('Press Ctrl+C to exit (will close browser).');

    // Keep alive
    await new Promise(() => {});

  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

main();
