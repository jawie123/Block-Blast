package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.BlockShape
import com.example.model.GameMode
import com.example.model.SavedGameSessionData
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local file storage for persisting active game sessions to app-private filesDir,
 * allowing instant resume on app launch even when offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalSessionStorage(context: Context) {

    private val sessionFile = File(context.applicationContext.filesDir, FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val writeLock = Any()
    private var cachedRootJson: JSONObject? = null

    @Synchronized
    fun saveSession(session: SavedGameSessionData) {
        try {
            val payload = updateCachedSessionJson(session)
            synchronized(writeLock) {
                sessionFile.writeText(payload)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save local game session", e)
        }
    }

    /**
     * Updates the in-memory session snapshot synchronously so any immediate reads stay consistent,
     * then writes to disk off the Main thread on a serialized IO lane.
     */
    @Synchronized
    fun saveSessionBackground(session: SavedGameSessionData) {
        try {
            val payload = updateCachedSessionJson(session)
            ioScope.launch {
                try {
                    synchronized(writeLock) {
                        sessionFile.writeText(payload)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save local game session asynchronously", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to serialize local game session", e)
        }
    }

    private fun updateCachedSessionJson(session: SavedGameSessionData): String {
        val root = loadRootJson()
        root.put(KEY_LAST_MODE, session.gameMode.name)
        val sessionsObj = root.optJSONObject(KEY_SESSIONS) ?: JSONObject()
        sessionsObj.put(session.gameMode.name, serializeSession(session))
        root.put(KEY_SESSIONS, sessionsObj)
        cachedRootJson = root
        return root.toString()
    }

    @Synchronized
    fun loadSession(mode: GameMode): SavedGameSessionData? {
        return try {
            val root = loadRootJson()
            val sessionsObj = root.optJSONObject(KEY_SESSIONS) ?: return null
            val json = sessionsObj.optJSONObject(mode.name) ?: return null
            deserializeSession(json)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load local session for $mode", e)
            null
        }
    }

    @Synchronized
    fun loadLatestSession(): SavedGameSessionData? {
        return try {
            val root = loadRootJson()
            val lastModeName = root.optString(KEY_LAST_MODE, "")
            val sessionsObj = root.optJSONObject(KEY_SESSIONS) ?: return null
            if (lastModeName.isNotEmpty() && sessionsObj.has(lastModeName)) {
                return deserializeSession(sessionsObj.getJSONObject(lastModeName))
            }
            val keys = sessionsObj.keys()
            var latest: SavedGameSessionData? = null
            while (keys.hasNext()) {
                val key = keys.next()
                val candidate = deserializeSession(sessionsObj.optJSONObject(key) ?: continue) ?: continue
                if (latest == null || candidate.clientUpdatedAt > latest.clientUpdatedAt) {
                    latest = candidate
                }
            }
            latest
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load latest local session", e)
            null
        }
    }

    @Synchronized
    fun clearSession(mode: GameMode) {
        try {
            val root = loadRootJson()
            val sessionsObj = root.optJSONObject(KEY_SESSIONS) ?: return
            sessionsObj.remove(mode.name)
            root.put(KEY_SESSIONS, sessionsObj)
            cachedRootJson = root
            val payload = root.toString()
            ioScope.launch {
                try {
                    synchronized(writeLock) {
                        sessionFile.writeText(payload)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear local session for $mode", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear local session for $mode", e)
        }
    }

    private fun loadRootJson(): JSONObject {
        cachedRootJson?.let { return it }
        if (!sessionFile.exists()) {
            return JSONObject().also { cachedRootJson = it }
        }
        val text = sessionFile.readText()
        val parsed = if (text.isBlank()) JSONObject() else JSONObject(text)
        cachedRootJson = parsed
        return parsed
    }

    private fun serializeSession(session: SavedGameSessionData): JSONObject {
        val gridArr = JSONArray()
        for (r in 0 until 8) {
            val rowArr = JSONArray()
            for (c in 0 until 8) {
                rowArr.put(session.grid.getOrNull(r)?.getOrNull(c) ?: 0)
            }
            gridArr.put(rowArr)
        }

        val piecesArr = JSONArray()
        for (i in 0 until 3) {
            val shape = session.activePieces.getOrNull(i)
            if (shape == null) {
                piecesArr.put(JSONObject.NULL)
            } else {
                val matrixArr = JSONArray()
                for (row in shape.matrix) {
                    val rowArr = JSONArray()
                    for (cell in row) {
                        rowArr.put(cell)
                    }
                    matrixArr.put(rowArr)
                }
                piecesArr.put(
                    JSONObject().apply {
                        put("instanceId", shape.instanceId)
                        put("id", shape.id)
                        put("name", shape.name)
                        put("colorId", shape.colorId)
                        put("matrix", matrixArr)
                    }
                )
            }
        }

        val gemsArr = JSONArray()
        for ((r, c) in session.gemCells) {
            gemsArr.put("$r,$c")
        }
        val iceObj = JSONObject()
        for ((coord, hits) in session.iceCells) {
            iceObj.put("${coord.first},${coord.second}", hits)
        }

        val lightningArr = JSONArray()
        for ((r, c) in session.lightningCells) {
            lightningArr.put("$r,$c")
        }

        val heldPieceJson: Any = session.heldPiece?.let { shape ->
            val matrixArr = JSONArray()
            for (row in shape.matrix) {
                val rowArr = JSONArray()
                for (cell in row) rowArr.put(cell)
                matrixArr.put(rowArr)
            }
            JSONObject().apply {
                put("instanceId", shape.instanceId)
                put("id", shape.id)
                put("name", shape.name)
                put("colorId", shape.colorId)
                put("matrix", matrixArr)
            }
        } ?: JSONObject.NULL

        return JSONObject().apply {
            put("gameMode", session.gameMode.name)
            put("grid", gridArr)
            put("activePieces", piecesArr)
            put("score", session.score)
            put("comboStreak", session.comboStreak)
            put("maxComboSession", session.maxComboSession)
            put("linesClearedSession", session.linesClearedSession)
            put("isGameOver", session.isGameOver)
            put("secondChanceAvailable", session.secondChanceAvailable)
            put("dailyDate", session.dailyDate)
            put("dailyBatchIndex", session.dailyBatchIndex)
            put("hammerCount", session.hammerCount)
            put("clientUpdatedAt", session.clientUpdatedAt)
            put("timeRemainingMs", session.timeRemainingMs)
            put("adventureLevelNumber", session.adventureLevelNumber)
            put("adventureGemsCollected", session.adventureGemsCollected)
            put("adventureIceBroken", session.adventureIceBroken)
            put("gemCells", gemsArr)
            put("iceCells", iceObj)
            put("heldPiece", heldPieceJson)
            put("canHoldThisTurn", session.canHoldThisTurn)
            put("lightningCells", lightningArr)
        }
    }

    private fun deserializeSession(json: JSONObject): SavedGameSessionData? {
        val modeName = json.optString("gameMode", GameMode.CLASSIC.name)
        val gameMode = runCatching { GameMode.valueOf(modeName) }.getOrElse { GameMode.CLASSIC }

        val gridArr = json.optJSONArray("grid") ?: return null
        if (gridArr.length() != 8) return null
        val grid = (0 until 8).map { r ->
            val rowArr = gridArr.optJSONArray(r) ?: return null
            if (rowArr.length() != 8) return null
            (0 until 8).map { c -> rowArr.optInt(c, 0) }
        }

        val piecesArr = json.optJSONArray("activePieces") ?: return null
        val pieces = (0 until 3).map { i ->
            if (i >= piecesArr.length() || piecesArr.isNull(i)) {
                null
            } else {
                val pObj = piecesArr.optJSONObject(i) ?: return@map null
                val id = pObj.optString("id", "")
                val name = pObj.optString("name", id)
                val colorId = pObj.optInt("colorId", 1)
                val instanceId = pObj.optString("instanceId", java.util.UUID.randomUUID().toString())
                val matrixArr = pObj.optJSONArray("matrix") ?: return@map null
                val matrix = (0 until matrixArr.length()).map { r ->
                    val rArr = matrixArr.getJSONArray(r)
                    (0 until rArr.length()).map { c -> rArr.getBoolean(c) }
                }
                if (matrix.isEmpty() || matrix[0].isEmpty()) null
                else BlockShape(
                    instanceId = instanceId,
                    id = id,
                    name = name,
                    matrix = matrix,
                    colorId = colorId
                )
            }
        }

        val gemSet = mutableSetOf<Pair<Int, Int>>()
        val gemsArr = json.optJSONArray("gemCells")
        if (gemsArr != null) {
            for (i in 0 until gemsArr.length()) {
                val parts = gemsArr.optString(i, "").split(",")
                if (parts.size == 2) {
                    val r = parts[0].toIntOrNull()
                    val c = parts[1].toIntOrNull()
                    if (r != null && c != null && r in 0..7 && c in 0..7) {
                        gemSet.add(r to c)
                    }
                }
            }
        }

        val iceMap = mutableMapOf<Pair<Int, Int>, Int>()
        val iceObj = json.optJSONObject("iceCells")
        if (iceObj != null) {
            val keys = iceObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val parts = key.split(",")
                if (parts.size == 2) {
                    val r = parts[0].toIntOrNull()
                    val c = parts[1].toIntOrNull()
                    val hits = iceObj.optInt(key, 1)
                    if (r != null && c != null && r in 0..7 && c in 0..7 && hits > 0) {
                        iceMap[r to c] = hits.coerceIn(1, 2)
                    }
                }
            }
        }

        val lightningSet = mutableSetOf<Pair<Int, Int>>()
        val lightningArr = json.optJSONArray("lightningCells")
        if (lightningArr != null) {
            for (i in 0 until lightningArr.length()) {
                val parts = lightningArr.optString(i, "").split(",")
                if (parts.size == 2) {
                    val r = parts[0].toIntOrNull()
                    val c = parts[1].toIntOrNull()
                    if (r != null && c != null && r in 0..7 && c in 0..7) {
                        lightningSet.add(r to c)
                    }
                }
            }
        }

        val heldShape: BlockShape? = json.optJSONObject("heldPiece")?.let { pObj ->
            val id = pObj.optString("id", "")
            val name = pObj.optString("name", id)
            val colorId = pObj.optInt("colorId", 1)
            val instanceId = pObj.optString("instanceId", java.util.UUID.randomUUID().toString())
            val matrixArr = pObj.optJSONArray("matrix")
            if (matrixArr == null) {
                null
            } else {
                val matrix = (0 until matrixArr.length()).map { r ->
                    val rArr = matrixArr.getJSONArray(r)
                    (0 until rArr.length()).map { c -> rArr.getBoolean(c) }
                }
                if (matrix.isEmpty() || matrix[0].isEmpty()) null
                else BlockShape(
                    instanceId = instanceId,
                    id = id,
                    name = name,
                    matrix = matrix,
                    colorId = colorId
                )
            }
        }

        return SavedGameSessionData(
            gameMode = gameMode,
            grid = grid,
            activePieces = pieces,
            score = json.optInt("score", 0),
            comboStreak = json.optInt("comboStreak", 0),
            maxComboSession = json.optInt("maxComboSession", 0),
            linesClearedSession = json.optInt("linesClearedSession", 0),
            isGameOver = json.optBoolean("isGameOver", false),
            secondChanceAvailable = json.optBoolean("secondChanceAvailable", true),
            dailyDate = json.optString("dailyDate", ""),
            dailyBatchIndex = json.optInt("dailyBatchIndex", 0),
            hammerCount = json.optInt("hammerCount", 3),
            clientUpdatedAt = json.optLong("clientUpdatedAt", 0L),
            timeRemainingMs = json.optLong("timeRemainingMs", 60_000L),
            adventureLevelNumber = json.optInt("adventureLevelNumber", 1).coerceAtLeast(1),
            adventureGemsCollected = json.optInt("adventureGemsCollected", 0).coerceAtLeast(0),
            adventureIceBroken = json.optInt("adventureIceBroken", 0).coerceAtLeast(0),
            gemCells = gemSet,
            iceCells = iceMap,
            heldPiece = heldShape,
            canHoldThisTurn = json.optBoolean("canHoldThisTurn", true),
            lightningCells = lightningSet
        )
    }

    private companion object {
        const val TAG = "LocalSessionStorage"
        const val FILE_NAME = "active_game_sessions.json"
        const val KEY_LAST_MODE = "lastActiveMode"
        const val KEY_SESSIONS = "sessions"
    }
}
