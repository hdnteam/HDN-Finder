package com.snifinder.app.util

/**
 * Massive SNI database - 500+ domains.
 * Includes Iranian sites, CDNs, cloud, gaming, streaming, dev, Google, Yahoo, and more.
 */
object SniListProvider {

    val defaultSniList = listOf(
        // ============================
        // === Iranian Websites ===
        // ============================
        "www.digikala.com",
        "api.digikala.com",
        "cdn.digikala.com",
        "file.digikala.com",
        "search.digikala.com",
        "www.divar.ir",
        "api.divar.ir",
        "s.divar.ir",
        "www.sheypoor.com",
        "www.torob.com",
        "api.torob.com",
        "www.basalam.com",
        "www.snappfood.ir",
        "snapp.ir",
        "app.snapp.taxi",
        "www.tapsi.ir",
        "www.alibaba.ir",
        "www.irancell.ir",
        "my.irancell.ir",
        "mci.ir",
        "my.mci.ir",
        "www.rightel.ir",
        "www.shaparak.ir",
        "www.namava.ir",
        "www.filimo.com",
        "www.aparat.com",
        "www.telewebion.com",
        "www.bale.ai",
        "web.bale.ai",
        "www.rubika.ir",
        "www.eitaa.com",
        "www.gap.im",
        "www.soroush-app.ir",
        "www.varzesh3.com",
        "www.zoomit.ir",
        "www.digiato.com",
        "www.techrato.com",
        "www.yjc.ir",
        "www.tabnak.ir",
        "www.khabaronline.ir",
        "www.isna.ir",
        "www.irna.ir",
        "www.mehrnews.com",
        "www.tasnimnews.com",
        "www.farsnews.ir",
        "www.iribnews.ir",
        "www.dana.ir",
        "www.entekhab.ir",
        "www.hamshahrionline.ir",
        "www.bartarinha.ir",
        "www.tebyan.net",
        "www.salameno.com",
        "www.chilivery.com",
        "www.taaghche.com",
        "www.fidibo.com",
        "www.ketabrah.ir",
        "cafebazaar.ir",
        "www.myket.ir",
        "www.bazar.ir",
        "www.anten.ir",
        "www.ninisite.com",
        "www.virgool.io",
        "virgool.io",
        "www.stackoverflow.blog",
        "www.quera.org",
        "quera.org",
        "www.jobinja.ir",
        "www.jobvision.ir",
        "www.e-estekhdam.com",
        "www.irantalent.com",
        "www.ponisha.ir",
        "www.parspack.com",
        "www.ir-tci.ir",
        "adsl.shatel.ir",
        "www.shatel.ir",
        "www.asiatech.ir",
        "www.hiweb.ir",
        "www.pishgaman.net",
        "www.samantel.ir",
        "www.mobinnet.ir",
        "www.tamin.ir",
        "www.emalls.ir",
        "www.techno-life.com",
        "www.mobile.ir",
        "www.gsm.ir",
        "www.banimode.com",
        "www.modiseh.com",
        "www.bamilo.com",
        "www.zoomg.ir",
        "www.arzdigital.com",
        "www.nobitex.ir",
        "www.wallex.ir",
        "www.ramzarz.me",
        "www.beytoote.com",
        "www.roozegaar.com",
        "www.namnak.com",
        "www.setare.com",
        "www.delgarm.com",
        "www.kojaro.com",
        "www.eligasht.com",
        "www.snapptrip.com",
        "www.flytoday.ir",
        "www.safarmarket.com",
        "www.trip.ir",
        "www.pintapin.com",
        "www.cbi.ir",
        "www.bmi.ir",
        "www.bankmellat.ir",
        "www.bsi.ir",
        "www.sb24.ir",
        "www.en-bank.com",
        "www.tejaratbank.ir",
        "www.banksepah.ir",
        "www.bpi.ir",
        "www.bankmelli-iran.com",
        "www.parsianbank.ir",
        "www.bki.ir",
        "www.saman.ir",

        // ============================
        // === Google (Extensive) ===
        // ============================
        "www.google.com",
        "google.com",
        "accounts.google.com",
        "mail.google.com",
        "drive.google.com",
        "docs.google.com",
        "sheets.google.com",
        "slides.google.com",
        "calendar.google.com",
        "meet.google.com",
        "chat.google.com",
        "photos.google.com",
        "maps.google.com",
        "translate.google.com",
        "play.google.com",
        "news.google.com",
        "books.google.com",
        "scholar.google.com",
        "cloud.google.com",
        "console.cloud.google.com",
        "firebase.google.com",
        "analytics.google.com",
        "ads.google.com",
        "adsense.google.com",
        "adwords.google.com",
        "search.google.com",
        "store.google.com",
        "one.google.com",
        "gemini.google.com",
        "ai.google.dev",
        "colab.research.google.com",
        "earth.google.com",
        "fonts.google.com",
        "developers.google.com",
        "android.com",
        "developer.android.com",
        "source.android.com",
        "dl.google.com",
        "update.googleapis.com",
        "clients1.google.com",
        "clients2.google.com",
        "clients3.google.com",
        "clients4.google.com",
        "www.gstatic.com",
        "ssl.gstatic.com",
        "fonts.gstatic.com",
        "encrypted-tbn0.gstatic.com",
        "lh3.googleusercontent.com",
        "lh4.googleusercontent.com",
        "lh5.googleusercontent.com",
        "storage.googleapis.com",
        "firebaseio.com",
        "firebaseapp.com",
        "cloudfunctions.net",
        "run.app",
        "appspot.com",
        "withgoogle.com",
        "dialogflow.cloud.google.com",
        "translate.googleapis.com",
        "fonts.googleapis.com",
        "ajax.googleapis.com",
        "maps.googleapis.com",
        "youtube.googleapis.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "studio.youtube.com",
        "i.ytimg.com",
        "i9.ytimg.com",
        "s.ytimg.com",
        "yt3.ggpht.com",
        "yt3.googleusercontent.com",
        "manifest.googlevideo.com",
        "rr1---sn-a5meknlz.googlevideo.com",
        "rr2---sn-a5meknlz.googlevideo.com",
        "redirector.googlevideo.com",
        "jnn-pa.googleapis.com",
        "blogger.com",
        "www.blogger.com",
        "blogspot.com",
        "sites.google.com",
        "groups.google.com",
        "classroom.google.com",
        "chromium.org",
        "www.chromium.org",
        "chrome.google.com",
        "chromewebstore.google.com",
        "lens.google.com",
        "keep.google.com",
        "contacts.google.com",

        // ============================
        // === Yahoo (Extensive) ===
        // ============================
        "www.yahoo.com",
        "yahoo.com",
        "mail.yahoo.com",
        "login.yahoo.com",
        "search.yahoo.com",
        "news.yahoo.com",
        "finance.yahoo.com",
        "sports.yahoo.com",
        "weather.yahoo.com",
        "answers.yahoo.com",
        "groups.yahoo.com",
        "s.yimg.com",
        "s1.yimg.com",
        "l.yimg.com",
        "ct.yimg.com",
        "ads.yahoo.com",
        "analytics.yahoo.com",
        "api.yahoo.com",
        "developer.yahoo.com",
        "flickr.com",
        "www.flickr.com",
        "live.staticflickr.com",
        "farm66.staticflickr.com",
        "tumblr.com",
        "www.tumblr.com",
        "assets.tumblr.com",
        "media.tumblr.com",

        // ============================
        // === Cloudflare ===
        // ============================
        "cp.cloudflare.com",
        "speed.cloudflare.com",
        "dash.cloudflare.com",
        "workers.dev",
        "pages.dev",
        "cdnjs.cloudflare.com",
        "one.one.one.one",
        "gateway.ai.cloudflare.com",
        "radar.cloudflare.com",
        "blog.cloudflare.com",
        "cloudflare-dns.com",

        // ============================
        // === Akamai ===
        // ============================
        "a248.e.akamai.net",
        "e9706.dscg.akamaiedge.net",
        "a1887.dscq.akamai.net",
        "edgekey.net",
        "akamaized.net",
        "akadns.net",
        "akamai.net",
        "akamaihd.net",

        // ============================
        // === Fastly ===
        // ============================
        "fastly.com",
        "global.ssl.fastly.net",
        "dualstack.github.map.fastly.net",
        "vimeo.fastly.net",
        "js.sentry-cdn.com",

        // ============================
        // === Amazon / AWS ===
        // ============================
        "d1.awsstatic.com",
        "d2908q01vomqb2.cloudfront.net",
        "cloudfront.net",
        "s3.amazonaws.com",
        "s3.us-east-1.amazonaws.com",
        "s3.eu-west-1.amazonaws.com",
        "ec2.amazonaws.com",
        "elasticbeanstalk.com",
        "execute-api.us-east-1.amazonaws.com",
        "checkip.amazonaws.com",
        "aws.amazon.com",
        "www.amazon.com",
        "m.media-amazon.com",
        "images-na.ssl-images-amazon.com",
        "images-eu.ssl-images-amazon.com",

        // ============================
        // === Microsoft / Azure ===
        // ============================
        "www.microsoft.com",
        "login.microsoftonline.com",
        "graph.microsoft.com",
        "azure.microsoft.com",
        "azurewebsites.net",
        "azureedge.net",
        "trafficmanager.net",
        "msedge.net",
        "visualstudio.com",
        "vo.msecnd.net",
        "ajax.aspnetcdn.com",
        "outlook.live.com",
        "outlook.office365.com",
        "onedrive.live.com",
        "cdn.office.net",
        "www.bing.com",
        "th.bing.com",
        "cn.bing.com",
        "copilot.microsoft.com",
        "www.msn.com",
        "linkedin.com",
        "www.linkedin.com",
        "static.licdn.com",
        "media.licdn.com",

        // ============================
        // === DigitalOcean / Hetzner / OVH / Vultr ===
        // ============================
        "digitaloceanspaces.com",
        "ondigitalocean.app",
        "nyc3.digitaloceanspaces.com",
        "hetzner.cloud",
        "static.hetzner.cloud",
        "speed.hetzner.de",
        "mirror.hetzner.de",
        "ovh.net",
        "proof.ovh.net",
        "www.ovhcloud.com",
        "ewr1.vultrobjects.com",
        "sgp1.vultrobjects.com",
        "linodeobjects.com",

        // ============================
        // === CDN Providers ===
        // ============================
        "cdn77.org",
        "c.cdn77.org",
        "kxcdn.com",
        "b-cdn.net",
        "bunnycdn.com",
        "stackpathcdn.com",
        "cdn.jsdelivr.net",
        "unpkg.com",
        "rawcdn.githack.com",
        "statically.io",
        "maxcdn.bootstrapcdn.com",
        "cdn.tailwindcss.com",

        // ============================
        // === Gaming ===
        // ============================
        "epicgames-download1.akamaized.net",
        "download.epicgames.com",
        "cdn.unrealengine.com",
        "steamcontent.com",
        "steamstatic.com",
        "steamcdn-a.akamaihd.net",
        "valve.net",
        "store.steampowered.com",
        "cdn.akamai.steamstatic.com",
        "cdn.riot.cloudflare.com",
        "riotgames.com",
        "blizzard.com",
        "cdn.blizzard.com",
        "us.cdn.blizzard.com",
        "eu.cdn.blizzard.com",
        "download.epicgames.com",
        "www.ea.com",
        "www.ubisoft.com",

        // ============================
        // === Streaming ===
        // ============================
        "www.netflix.com",
        "nflxvideo.net",
        "assets.nflxext.com",
        "www.spotify.com",
        "scdn.co",
        "i.scdn.co",
        "audio-sp-tyo.spotifycdn.com",
        "audio-fa.spotifycdn.com",
        "www.twitch.tv",
        "static.twitchcdn.net",
        "video-weaver.hls.ttvnw.net",
        "usher.ttvnw.net",
        "www.disneyplus.com",
        "www.hulu.com",
        "www.hbomax.com",
        "www.primevideo.com",
        "www.soundcloud.com",
        "a-v2.sndcdn.com",
        "www.deezer.com",

        // ============================
        // === Communication ===
        // ============================
        "discord.com",
        "cdn.discordapp.com",
        "discord.media",
        "gateway.discord.gg",
        "images-ext-1.discordapp.net",
        "media.discordapp.net",
        "signal.org",
        "updates.signal.org",
        "cdn.signal.org",
        "static.whatsapp.net",
        "mmg.whatsapp.net",
        "web.whatsapp.com",
        "pps.whatsapp.net",
        "web.telegram.org",
        "telegram.org",
        "core.telegram.org",
        "t.me",
        "api.telegram.org",
        "cdn5.telegram-cdn.org",
        "telegram-cdn.org",
        "updates.tdesktop.com",
        "media.tenor.com",
        "www.skype.com",
        "teams.microsoft.com",
        "zoom.us",
        "us02web.zoom.us",
        "www.webex.com",

        // ============================
        // === Social Media ===
        // ============================
        "www.instagram.com",
        "i.instagram.com",
        "scontent.cdninstagram.com",
        "static.cdninstagram.com",
        "www.facebook.com",
        "static.xx.fbcdn.net",
        "scontent.xx.fbcdn.net",
        "connect.facebook.net",
        "www.twitter.com",
        "twitter.com",
        "x.com",
        "abs.twimg.com",
        "pbs.twimg.com",
        "video.twimg.com",
        "api.x.com",
        "www.tiktok.com",
        "v16-webapp.tiktok.com",
        "sf16-website-login.neutral.ttwstatic.com",
        "www.pinterest.com",
        "i.pinimg.com",
        "www.reddit.com",
        "i.redd.it",
        "v.redd.it",
        "external-preview.redd.it",
        "www.snapchat.com",
        "web.snapchat.com",

        // ============================
        // === Developer Platforms ===
        // ============================
        "github.com",
        "api.github.com",
        "raw.githubusercontent.com",
        "github.githubassets.com",
        "objects-origin.githubusercontent.com",
        "codeload.github.com",
        "ghcr.io",
        "gist.github.com",
        "gitlab.com",
        "registry.gitlab.com",
        "registry.npmjs.org",
        "pypi.org",
        "files.pythonhosted.org",
        "rubygems.org",
        "crates.io",
        "static.crates.io",
        "hub.docker.com",
        "registry-1.docker.io",
        "production.cloudflare.docker.com",
        "pkg.go.dev",
        "proxy.golang.org",
        "nuget.org",
        "api.nuget.org",
        "stackoverflow.com",
        "cdn.sstatic.net",
        "bitbucket.org",

        // ============================
        // === Mozilla ===
        // ============================
        "www.mozilla.org",
        "addons.mozilla.org",
        "cdn.mozilla.net",
        "services.mozilla.com",
        "detectportal.firefox.com",
        "ftp.mozilla.org",
        "archive.mozilla.org",

        // ============================
        // === News / Media ===
        // ============================
        "cdn.cnn.com",
        "static.bbc.co.uk",
        "www.bbc.com",
        "static.reuters.com",
        "www.reuters.com",
        "assets.bwbx.io",
        "cdn.vox-cdn.com",
        "static01.nyt.com",
        "www.nytimes.com",
        "www.theguardian.com",
        "assets.wired.com",
        "techcrunch.com",
        "www.washingtonpost.com",
        "www.aljazeera.com",
        "www.dw.com",

        // ============================
        // === Education ===
        // ============================
        "www.coursera.org",
        "www.edx.org",
        "www.khanacademy.org",
        "cdn.kastatic.org",
        "www.udemy.com",
        "www.duolingo.com",
        "d35aaqx5ub95lt.cloudfront.net",
        "www.codecademy.com",
        "www.mit.edu",
        "ocw.mit.edu",
        "www.harvard.edu",
        "www.stanford.edu",
        "www.w3schools.com",
        "www.freecodecamp.org",

        // ============================
        // === E-commerce ===
        // ============================
        "cdn.shopify.com",
        "www.shopify.com",
        "www.ebay.com",
        "www.aliexpress.com",
        "ae01.alicdn.com",
        "www.etsy.com",
        "www.walmart.com",

        // ============================
        // === Cloud Storage ===
        // ============================
        "www.dropbox.com",
        "dl.dropboxusercontent.com",
        "cfl.dropboxstatic.com",
        "www.box.com",
        "mega.nz",
        "g.api.mega.co.nz",
        "www.mediafire.com",
        "www.4shared.com",

        // ============================
        // === Security / VPN / DNS ===
        // ============================
        "api.nordvpn.com",
        "downloads.nordcdn.com",
        "mullvad.net",
        "protonvpn.com",
        "proton.me",
        "mail.proton.me",
        "www.expressvpn.com",
        "www.surfshark.com",
        "letsencrypt.org",
        "www.eff.org",
        "dns.google",
        "dns.cloudflare.com",
        "doh.opendns.com",
        "dns.adguard.com",
        "dns.quad9.net",
        "dns.nextdns.io",

        // ============================
        // === Speed Test ===
        // ============================
        "www.speedtest.net",
        "speed.hetzner.de",
        "proof.ovh.net",
        "speedtest.tele2.net",
        "ping.online.net",
        "lg.he.net",
        "speedtest.serverius.net",
        "speedtest.belwue.net",
        "bouygues.iperf.fr",

        // ============================
        // === AI / ML ===
        // ============================
        "api.openai.com",
        "cdn.openai.com",
        "chat.openai.com",
        "www.anthropic.com",
        "claude.ai",
        "huggingface.co",
        "cdn-lfs.huggingface.co",
        "www.kaggle.com",
        "www.perplexity.ai",

        // ============================
        // === Hosting / Platforms ===
        // ============================
        "www.netlify.com",
        "app.netlify.com",
        "www.vercel.com",
        "vercel.app",
        "www.heroku.com",
        "www.railway.app",
        "www.render.com",
        "fly.io",
        "www.squarespace.com",
        "www.wix.com",
        "www.wordpress.com",
        "s0.wp.com",
        "i0.wp.com",

        // ============================
        // === IP Info ===
        // ============================
        "ipinfo.io",
        "api.ipify.org",
        "ifconfig.me",
        "icanhazip.com",
        "api.ip.sb",
        "ip-api.com",
        "ipapi.co",
        "api.myip.com",
        "httpbin.org",
        "wtfismyip.com",

        // ============================
        // === Misc Popular ===
        // ============================
        "www.wikipedia.org",
        "en.wikipedia.org",
        "upload.wikimedia.org",
        "www.archive.org",
        "web.archive.org",
        "www.duckduckgo.com",
        "brave.com",
        "www.notion.so",
        "www.figma.com",
        "www.canva.com",
        "www.slack.com",
        "www.trello.com",
        "www.zoom.us",
        "images.unsplash.com",
        "cdn.pixabay.com",

        // ============================
        // === Apple / iCloud ===
        // ============================
        "icloud.com",
        "www.icloud.com",
        "www.apple.com",
        "apps.apple.com",
        "itunes.apple.com",
        "push.apple.com",
        "updates-http.cdn-apple.com",

        // ============================
        // === Crypto ===
        // ============================
        "api.binance.com",
        "www.coinbase.com",
        "api.coingecko.com",
        "pro-api.coinmarketcap.com",

        // ============================
        // === Fonts / Static ===
        // ============================
        "use.fontawesome.com",
        "kit.fontawesome.com",
        "use.typekit.net",

        // ============================
        // === Analytics ===
        // ============================
        "www.googletagmanager.com",
        "www.google-analytics.com",
        "cdn.segment.com",
        "plausible.io",

        // ============================
        // === Email ===
        // ============================
        "outlook.office365.com",
        "smtp.gmail.com",
        "imap.gmail.com",
        "mail.zoho.com"
    )

    /**
     * Parse user-provided SNI list (one per line or comma-separated)
     */
    fun parseUserList(input: String): List<String> {
        return input
            .replace(",", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains(".") }
            .distinct()
    }
}
