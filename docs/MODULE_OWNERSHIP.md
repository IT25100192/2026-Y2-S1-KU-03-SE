# Module ownership and git workflow

Group 2026-Y2-S1-KU-03. Read this before your first push.

## Who owns what

Each member owns one folder under `src/modules/`. You edit your own four
files freely. You do not edit anyone else's module files, ever - if you need
something from another module, ask that person to export a function for you.

### Balasuriya B.A.K.K. - IT25100192 - User Management

```
src/modules/user/user.model.js
src/modules/user/user.service.js
src/modules/user/user.controller.js
src/modules/user/user.routes.js
src/middleware/auth.js            (shared, but this module owns it)
```

Done: UM01 register, UM02 login, UM03 verify account, UM04 view and update
profile, UM05 password reset, UM06 list and suspend accounts.

Left: `changeRole()` with an audit trail entry, and soft delete or
anonymisation on account closure. Both need an `AuditLog` model that does
not exist yet.

`middleware/auth.js` sits outside the module folder because all six routers
import it. It is still yours. If you change the shape of `req.user`, tell the
group first - every other module reads it.

### Ahamed M.N.N. - IT25100183 - Voting Management

```
src/modules/voting/voting.model.js
src/modules/voting/voting.service.js
src/modules/voting/voting.controller.js
src/modules/voting/voting.routes.js
```

Done: VM01 cast a vote, VM02 eligibility and quota check, VM03 live tally
with free/paid breakdown, VM04 open and close a round, VM05 voting history
and vote voiding.

Left: VM06 fraud detection - one IP casting for many accounts, vote bursts
faster than a threshold per minute, accounts that register and vote within
the same minute. Then auto-void the flagged votes and raise an admin
notification. A rate limiter on `POST /api/votes` goes with it.

Your `castVote()` is the integration point for the whole system. If you
change what it calls or in what order, say so in the group chat, because it
is the function the demo walks through.

### De Silva T.J.T. - IT25100158 - Notification Management

```
src/modules/notification/notification.model.js
src/modules/notification/notification.templates.js
src/modules/notification/notification.service.js
src/modules/notification/notification.controller.js
src/modules/notification/notification.routes.js
```

Done: NM01 vote confirmations, NM02 verification and reset codes, NM03
broadcast announcements, NM04 channel preferences, NM05 history with read
and read-all.

Left: NM06 retry queue - pick up rows with `status: FAILED` and
`attempts < 3`, resend with backoff between attempts, and a `node-cron`
worker that runs it every five minutes. Swapping `deliver()` for a real
SendGrid or Twilio client goes with it.

Five modules call your `dispatch()`. Keep its signature stable
(`{ userId, template, payload, broadcast, overrideChannels }`) or five
people break at once. Adding a new template to `notification.templates.js`
is safe and needs no coordination, as long as you also add the name to
`NOTIFICATION_TEMPLATE` in `src/config/constants.js`.

### Siriwardhana W.M.B.B.B. - IT25100187 - Contestant Management

```
src/modules/contestant/contestant.model.js
src/modules/contestant/contestant.service.js
src/modules/contestant/contestant.controller.js
src/modules/contestant/contestant.routes.js
```

Done: CM01 register contestant, CM02 update profile, CM03 seasons, rounds
and line-ups, CM04 leaderboard with vote share, CM05 publish results with
advance and eliminate.

Left: CM06 performance media upload - a `multer` upload of the round clip
stored per `RoundEntry`, with a size and mime-type check. Judge scoring
alongside the public vote is in the proposal at section 3.4 but is out of
scope for this sprint.

You own four collections, more than anyone else: `Season`, `Round`,
`Contestant` and `RoundEntry`. Voting Management calls three of your
functions - `assertVotable()`, `addVotes()` and `setRoundStatus()`. Those
three are your public contract. The rest of the file you can change freely.

### Nawarathna I.K.H.G.B. - IT25100174 - Payment Management

```
src/modules/payment/payment.model.js
src/modules/payment/payment.service.js
src/modules/payment/payment.controller.js
src/modules/payment/payment.routes.js
```

Done: PM01 bundle catalogue, PM02 purchase with gateway simulation, PM03
transaction history for voter and admin, PM04 refund with a guard against
refunding spent credits, PM05 receipt.

Left: PM06 daily reconciliation against a gateway settlement file, PDF
receipt rendering with `pdfkit` instead of the JSON object, and a webhook
endpoint so the gateway can confirm asynchronously rather than the app
trusting the synchronous response.

`callGateway()` is the only function that has to change when a live
PayHere or WebXPay merchant account is ready. Keep it that way - do not let
gateway details leak into `purchaseBundle()`.

Voting Management calls your `consumeCredits()` and `refundCredits()`.
Those two must stay synchronous and must throw rather than return an error
object, because `castVote()` relies on the throw to abort the vote.

### Weerasinghe W.A.D.S. - IT25100176 - Sponsor & Partnership Management

```
src/modules/sponsor/sponsor.model.js
src/modules/sponsor/sponsor.service.js
src/modules/sponsor/sponsor.controller.js
src/modules/sponsor/sponsor.routes.js
```

Done: SM01 register and manage sponsors, SM02 package rate card, SM03
agreements with activate and terminate, SM04 banner delivery with
impression logging, SM05 exposure report with fulfilment percentage,
click-through rate and cost per impression.

Left: SM06 renewal - clone an expiring agreement into the next season with
an uplift percentage and mark the old one `EXPIRED`, plus a nightly job that
flips `ACTIVE` to `EXPIRED` once `endsAt` passes. Invoicing against
`contractValueLKR` needs a group decision first: reuse Payment Management,
or keep sponsor money separate from voter money.

Voting Management calls your `getBannersForRound()` from inside the live
tally, so if it throws, the leaderboard breaks. Fail soft in there.

## Shared files

These four belong to everyone, which means nobody changes them alone.

```
src/config/constants.js     enums and env config
src/routes/index.js         the six mount points
src/app.js                  express setup and middleware order
package.json                dependencies
```

Adding a value to an existing enum in `constants.js` is fine, do it and
mention it. Renaming or removing one is not - it silently breaks whichever
module was matching on the old string.

`routes/index.js` is the file most likely to conflict on merge. It only
changes when someone adds a whole new mount point, which should be rare now
that all six are mounted.

If you add a dependency, run `npm install <pkg>` and commit both
`package.json` and `package-lock.json` in the same commit. Committing one
without the other is what causes "works on my machine".

## Branching

One branch per person, named after the module, not the person:

```
main
├── feat/user-management
├── feat/voting-management
├── feat/notification-management
├── feat/contestant-management
├── feat/payment-management
└── feat/sponsor-management
```

Work on your branch. Open a pull request into `main` when a use case is
finished, not when the whole module is. Small pull requests get reviewed;
big ones get rubber-stamped, which defeats the point.

Nobody pushes straight to `main`. Every pull request needs one approval from
somebody else in the group - it is also the evidence of peer review that
SE2030 asks for.

Before you open a pull request:

```bash
git checkout main && git pull
git checkout feat/your-module
git rebase main
npm run seed        # must finish and print a leaderboard
```

If the seed script fails after your change, your change broke someone else's
module. Fix it before the pull request, not after.

## Commit messages

Tag the use case. It makes the traceability matrix in the report almost
write itself.

```
UM03: verify account with a 6-digit code
VM06: flag votes from one IP across multiple accounts
PM04: block refunds once credits have been spent
```

## First push

The repo is currently one commit on `main`. To split it the way the
assignment expects:

```bash
git init
git add .
git commit -m "Initial project structure and six module skeletons"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main

git checkout -b feat/user-management     # each member, their own
git push -u origin feat/user-management
```

Everyone clones the same repo. You do not each push a separate file into a
shared folder - you each push commits to your own branch of the same repo,
and the merge history is what gets marked.
