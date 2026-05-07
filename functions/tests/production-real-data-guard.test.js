const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const repoRoot = path.resolve(__dirname, "..", "..");

const productionContentFiles = [
  "functions/src/founder/briefing-enrichment.js",
  "androidApp/src/main/java/com/nash/skyos/ui/model/MusicUiState.kt",
  "androidApp/src/main/java/com/nash/skyos/ui/viewmodel/HomeViewModel.kt",
  "androidApp/src/main/java/com/nash/skyos/data/repository/AndroidMusicRepository.kt",
  "androidApp/src/main/java/com/nash/skyos/ui/SkydownApp.kt",
  "androidApp/src/main/java/com/nash/skyos/ui/model/VideoHubPublicConfig.kt",
  "shared/src/commonMain/kotlin/com/skydown/shared/usecase/MusicUseCase.kt",
  "Skydown App/Services/Music/SpotifyMusicServiceSupport.swift",
  "Skydown App/Views/Music/MusicView.swift",
  "Skydown App/Views/MainTabView.swift",
  "Skydown App/Views/Music/SubView/MusicSharedComponents.swift",
  "Skydown App/ViewModels/Merchandise/MerchandiseViewModel.swift",
  "Skydown App/ViewModels/Music/VideoHubPublicConfig.swift",
];

const approvedZweizweiArtists = [
  "JANNO",
  "MAVE040",
  "Tangajoe007",
  "DANGU61",
  "Yang D. Nash",
  "ThaDude",
];

test("production content paths do not reintroduce legacy demo artists", () => {
  const forbidden = /\b(Toprack941|Toprack)\b/;
  const offenders = productionContentFiles.flatMap((relativePath) => {
    const absolutePath = path.join(repoRoot, relativePath);
    const content = fs.readFileSync(absolutePath, "utf8");
    return forbidden.test(content) ? [relativePath] : [];
  });

  assert.deepEqual(offenders, []);
});

test("music hub keeps the approved artist order on iOS and Android", () => {
  const iosContent = fs.readFileSync(
      path.join(repoRoot, "Skydown App/Views/Music/SubView/MusicSharedComponents.swift"),
      "utf8",
  );
  const androidContent = fs.readFileSync(
      path.join(repoRoot, "androidApp/src/main/java/com/nash/skyos/ui/model/MusicArtistCatalog.kt"),
      "utf8",
  );

  const iosArtists = iosContent
      .match(/let zweizweiCanonicalArtists = \[([\s\S]*?)\]/)[1]
      .match(/"([^"]+)"/g)
      .map((value) => value.replaceAll("\"", ""));
  const androidArtists = androidContent
      .match(/val defaultZweizweiMusicArtists = listOf\(([\s\S]*?)\)/)[1]
      .match(/"([^"]+)"/g)
      .map((value) => value.replaceAll("\"", ""));

  assert.deepEqual(iosArtists, approvedZweizweiArtists);
  assert.deepEqual(androidArtists, approvedZweizweiArtists);
});

test("music hub keeps Mave and ThaDude on their exact public profiles", () => {
  const androidContent = fs.readFileSync(
      path.join(repoRoot, "androidApp/src/main/java/com/nash/skyos/ui/SkydownApp.kt"),
      "utf8",
  );
  const androidCatalogContent = fs.readFileSync(
      path.join(repoRoot, "androidApp/src/main/java/com/nash/skyos/ui/model/MusicArtistCatalog.kt"),
      "utf8",
  );
  const iosContent = fs.readFileSync(
      path.join(repoRoot, "Skydown App/Views/Music/SubView/MusicSharedComponents.swift"),
      "utf8",
  );
  const iosSpotifyContent = fs.readFileSync(
      path.join(repoRoot, "Skydown App/Services/Music/SpotifyMusicServiceSupport.swift"),
      "utf8",
  );

  for (const content of [androidContent, iosContent]) {
    assert.match(content, /https:\/\/www\.instagram\.com\/mave040_official\//);
    assert.match(content, /https:\/\/www\.instagram\.com\/thadude_offizielle\//);
  }

  for (const content of [androidContent, androidCatalogContent, iosContent, iosSpotifyContent]) {
    assert.match(content, /https:\/\/open\.spotify\.com\/artist\/0GXymtRaIk2ngbXSkcHtsp|0GXymtRaIk2ngbXSkcHtsp/);
    assert.match(content, /https:\/\/open\.spotify\.com\/artist\/0Jmb7DXFkKxxRjqD70vi0e|0Jmb7DXFkKxxRjqD70vi0e/);
  }
});
