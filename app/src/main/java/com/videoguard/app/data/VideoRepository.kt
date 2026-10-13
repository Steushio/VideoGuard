package com.videoguard.app.data

import com.videoguard.app.model.VideoItem

object VideoRepository {

    val sampleVideos = listOf(
        VideoItem(
            id = "4NRXx6U8ABQ",
            title = "The Weeknd - Blinding Lights (Official Video)",
            channelName = "The Weeknd",
            channelAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150",
            subscriberCount = "34.8M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
            views = "820M views",
            uploadTime = "4 years ago",
            duration = "4:20",
            description = "Official music video for The Weeknd 'Blinding Lights' - available everywhere now. Taken from the studio album After Hours.",
            likes = "11M",
            isMusicTrack = true,
            artist = "The Weeknd",
            album = "After Hours",
            lyrics = """
                Yeah
                I've been tryin' to call
                I've been on my own for long enough
                Maybe you can show me how to love, maybe
                
                I'm going through withdrawals
                You don't even have to do too much
                You can turn me on with just a touch, baby
                
                I look around and Sin City's cold and empty
                No one's around to judge me
                I can't see clearly when you're gone
                
                I said, ooh, I'm blinded by the lights
                No, I can't sleep until I feel your touch
                I said, ooh, I'm drowning in the night
                Oh, when I'm like this, you're the one I trust
            """.trimIndent(),
            category = "Music",
            mood = "Energize"
        ),
        VideoItem(
            id = "dvgZkm1xWPE",
            title = "Coldplay - Viva La Vida (Official Video)",
            channelName = "Coldplay",
            channelAvatarUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=150",
            subscriberCount = "26.1M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/dvgZkm1xWPE/hqdefault.jpg",
            views = "860M views",
            uploadTime = "15 years ago",
            duration = "4:03",
            description = "The official music video for Coldplay - Viva La Vida. Taken from the album Viva La Vida or Death and All His Friends.",
            likes = "5.8M",
            isMusicTrack = true,
            artist = "Coldplay",
            album = "Viva La Vida",
            lyrics = """
                I used to rule the world
                Seas would rise when I gave the word
                Now in the morning I sleep alone
                Sweep the streets I used to own

                I used to roll the dice
                Feel the fear in my enemy's eyes
                Listen as the crowd would sing
                Now the old king is dead, long live the king
            """.trimIndent(),
            category = "Music",
            mood = "Relax"
        ),
        VideoItem(
            id = "7wtfhZwyrcc",
            title = "Imagine Dragons - Believer (Official Music Video)",
            channelName = "ImagineDragons",
            channelAvatarUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=150",
            subscriberCount = "31.2M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/7wtfhZwyrcc/hqdefault.jpg",
            views = "2.6B views",
            uploadTime = "7 years ago",
            duration = "3:37",
            description = "Listen to 'Believer' by Imagine Dragons, off their album EVOLVE.",
            likes = "22M",
            isMusicTrack = true,
            artist = "Imagine Dragons",
            album = "Evolve",
            lyrics = """
                First things first
                I'ma say all the words inside my head
                I'm fired up and tired of the way that things have been, oh-ooh
                The way that things have been, oh-ooh
                
                Second thing second
                Don't you tell me what you think that I can be
                I'm the one at the sail, I'm the master of my sea, oh-ooh
                The master of my sea, oh-ooh
                
                Pain!
                You made me a, you made me a believer, believer
                Pain!
                You break me down, you build me up, believer, believer
            """.trimIndent(),
            category = "Music",
            mood = "Workout"
        ),
        VideoItem(
            id = "BC19kwABFwc",
            title = "Dua Lipa - Levitating (Official Music Video)",
            channelName = "Dua Lipa",
            channelAvatarUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=150",
            subscriberCount = "23.4M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/BC19kwABFwc/hqdefault.jpg",
            views = "890M views",
            uploadTime = "3 years ago",
            duration = "3:51",
            description = "The official music video for Dua Lipa - Levitating from her studio album Future Nostalgia.",
            likes = "6.4M",
            isMusicTrack = true,
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            lyrics = """
                If you wanna run away with me, I know a galaxy
                And I can take you for a ride
                I had a premonition that we fell into a rhythm
                Where the music don't stop for life
                
                Glitter in the sky, glitter in my eyes
                Shining just the way I like
                If you're feeling like you need a little bit of company
                You met me at the perfect time
                
                You want me, I want you, baby
                My sugarboo, I'm levitating
                The Milky Way, we're renegading
                Yeah, yeah, yeah, yeah, yeah
            """.trimIndent(),
            category = "Music",
            mood = "Party"
        ),
        VideoItem(
            id = "OPf0YbXqDm0",
            title = "Mark Ronson - Uptown Funk ft. Bruno Mars (Official Video)",
            channelName = "Mark Ronson",
            channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            subscriberCount = "19.5M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/OPf0YbXqDm0/hqdefault.jpg",
            views = "5.1B views",
            uploadTime = "9 years ago",
            duration = "4:30",
            description = "Official Video for 'Uptown Funk' by Mark Ronson feat. Bruno Mars.",
            likes = "21M",
            isMusicTrack = true,
            artist = "Mark Ronson ft. Bruno Mars",
            album = "Uptown Special",
            lyrics = """
                This hit, that ice cold
                Michelle Pfeiffer, that white gold
                This one for them hood girls
                Them good girls straight masterpieces
                Stylin', wilin', livin' it up in the city
                Got Chucks on with Saint Laurent
                Gotta kiss myself, I'm so pretty
            """.trimIndent(),
            category = "Music",
            mood = "Party"
        ),
        VideoItem(
            id = "094y1Z2wpJg",
            title = "The Simplest Math Problem No One Can Solve - Collatz Conjecture",
            channelName = "Veritasium",
            channelAvatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150",
            subscriberCount = "15.7M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/094y1Z2wpJg/hqdefault.jpg",
            views = "41M views",
            uploadTime = "2 years ago",
            duration = "22:08",
            description = "The Collatz Conjecture is the simplest math problem that no one has solved. Pick a number, any number...",
            likes = "1.9M",
            isMusicTrack = false,
            category = "Podcasts"
        ),
        VideoItem(
            id = "JyECrGp-Sw8",
            title = "What If We Detonated All Nuclear Bombs at Once?",
            channelName = "Kurzgesagt – In a Nutshell",
            channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            subscriberCount = "22.5M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/JyECrGp-Sw8/hqdefault.jpg",
            views = "32M views",
            uploadTime = "5 years ago",
            duration = "7:12",
            description = "There are about 15,000 nuclear weapons on Earth. What would happen if we put them all in one spot and detonated them together?",
            likes = "1.5M",
            isMusicTrack = false,
            category = "Trending"
        ),
        VideoItem(
            id = "JGwWNGJdvx8",
            title = "Ed Sheeran - Shape of You (Official Music Video)",
            channelName = "Ed Sheeran",
            channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
            subscriberCount = "55.1M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/JGwWNGJdvx8/hqdefault.jpg",
            views = "6.2B views",
            uploadTime = "7 years ago",
            duration = "4:23",
            description = "The official music video for Ed Sheeran - Shape of You. From the album ÷ (Divide).",
            likes = "32M",
            isMusicTrack = true,
            artist = "Ed Sheeran",
            album = "Divide",
            lyrics = """
                The club isn't the best place to find a lover
                So the bar is where I go
                Me and my friends at the table doing shots
                Drinking fast and then we talk slow
                
                Come over and start up a conversation with just me
                And trust me I'll give it a chance now
                Take my hand, stop, put Van the Man on the jukebox
                And then we start to dance
                
                Girl, you know I want your love
                Your love was handmade for somebody like me
                Come on now, follow my lead
                I may be crazy, don't mind me
            """.trimIndent(),
            category = "Music",
            mood = "Energize"
        ),
        VideoItem(
            id = "fJ9rUzIMcZQ",
            title = "Queen – Bohemian Rhapsody (Official Video Remastered)",
            channelName = "Queen Official",
            channelAvatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
            subscriberCount = "17.8M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            views = "1.7B views",
            uploadTime = "15 years ago",
            duration = "5:59",
            description = "Taken from A Night At The Opera, 1975. The official Bohemian Rhapsody music video.",
            likes = "12M",
            isMusicTrack = true,
            artist = "Queen",
            album = "A Night at the Opera",
            lyrics = """
                Is this the real life?
                Is this just fantasy?
                Caught in a landslide, no escape from reality
                Open your eyes, look up to the skies and see
                
                I'm just a poor boy, I need no sympathy
                Because I'm easy come, easy go, little high, little low
                Any way the wind blows doesn't really matter to me, to me
                
                Mama, just killed a man
                Put a gun against his head, pulled my trigger, now he's dead
                Mama, life had just begun
                But now I've gone and thrown it all away
            """.trimIndent(),
            category = "Music",
            mood = "Focus"
        ),
        VideoItem(
            id = "dQw4w9WgXcQ",
            title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
            channelName = "Rick Astley",
            channelAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150",
            subscriberCount = "4.3M subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            views = "1.5B views",
            uploadTime = "14 years ago",
            duration = "3:33",
            description = "The official video for 'Never Gonna Give You Up' by Rick Astley. Taken from the album 'Whenever You Need Somebody'.",
            likes = "16M",
            isMusicTrack = true,
            artist = "Rick Astley",
            album = "Whenever You Need Somebody",
            lyrics = """
                We're no strangers to love
                You know the rules and so do I
                A full commitment's what I'm thinking of
                You wouldn't get this from any other guy
                
                I just wanna tell you how I'm feeling
                Gotta make you understand
                
                Never gonna give you up
                Never gonna let you down
                Never gonna run around and desert you
                Never gonna make you cry
                Never gonna say goodbye
                Never gonna tell a lie and hurt you
            """.trimIndent(),
            category = "Music",
            mood = "Commute"
        )
    )

    val musicCategories = listOf(
        "Energize", "Workout", "Relax", "Focus", "Commute", "Party"
    )

    val youtubeCategories = listOf(
        "All", "Music", "Podcasts", "Gaming", "News", "Tech", "Trending", "Recently uploaded"
    )

    fun getMusicTracks(): List<VideoItem> {
        return sampleVideos.filter { it.isMusicTrack }
    }

    fun getVideosByCategory(category: String): List<VideoItem> {
        if (category == "All") return sampleVideos
        return sampleVideos.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun getMusicByMood(mood: String): List<VideoItem> {
        return sampleVideos.filter { it.isMusicTrack && it.mood.equals(mood, ignoreCase = true) }.ifEmpty {
            getMusicTracks()
        }
    }

    fun searchVideos(query: String): List<VideoItem> {
        if (query.isBlank()) return sampleVideos
        val q = query.trim().lowercase()
        return sampleVideos.filter {
            it.title.lowercase().contains(q) ||
            it.channelName.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    fun searchMusic(query: String): List<VideoItem> {
        val music = getMusicTracks()
        if (query.isBlank()) return music
        val q = query.trim().lowercase()
        return music.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.mood.lowercase().contains(q)
        }
    }
}
