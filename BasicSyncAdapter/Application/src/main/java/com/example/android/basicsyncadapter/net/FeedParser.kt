/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")

package com.example.android.basicsyncadapter.net

import android.text.format.Time
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.ParseException

/**
 * This class parses generic Atom feeds. Given an InputStream representation of a feed, it returns
 * a List of entries, where each list element represents a single entry (post) in the XML feed.
 *
 * An example of an Atom feed can be found at:
 * http://en.wikipedia.org/w/index.php?title=Atom_(standard)&oldid=560239173#Example_of_an_Atom_1.0_feed
 */
class FeedParser {
    /**
     * Parse an Atom feed, returning a collection of [Entry] objects. First we initialize our
     * [XmlPullParser] variable `val parser` with a new instance. We disable its
     * `FEATURE_PROCESS_NAMESPACES` feature, set the input stream it is going to process to our
     * [InputStream] parameter [inputStream], and skip white space until it has reached the first
     * xml start tag or end tag. Finally we return the list of [Entry] objects returned by our
     * method [readFeed] when it is called for `parser`.
     *
     * @param inputStream Atom feed, as a stream.
     * @return [List] of [Entry] objects.
     * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
     * @throws java.io.IOException on I/O error.
     */
    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    fun parse(inputStream: InputStream): List<Entry> {
        return inputStream.use { stream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)
            parser.nextTag()
            readFeed(parser)
        }
    }

    /**
     * Decode a feed attached to an [XmlPullParser]. First we initialize [List] of [Entry] variable
     * `val entries` with a new instance of [ArrayList]. Next we check if the current event is a
     * `START_TAG` with the name "feed" (throwing [XmlPullParserException] if it is not) then while
     * the next parsing event is not a `END_TAG` we loop, setting [String] variable `val name` to the
     * name of the current element. If this `name` is equal to "entry" we add the value returned by
     * our method [readEntry] when it reads from [parser], otherwise we skip this tag. When we reach
     * the `END_TAG` for the "feed" element we stop looping and return `entries` to the caller.
     *
     * @param parser Incoming XMl
     * @return [List] of [Entry] objects.
     * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
     * @throws java.io.IOException on I/O error.
     */
    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    private fun readFeed(parser: XmlPullParser): List<Entry> {
        val entries: MutableList<Entry> = ArrayList()

        // Search for <feed> tags. These wrap the beginning/end of an Atom document.
        //
        // Example:
        // <?xml version="1.0" encoding="utf-8"?>
        // <feed xmlns="http://www.w3.org/2005/Atom">
        // ...
        // </feed> TODO: Figure out why require does not see the "feed" tag (it is there!)
        parser.require(XmlPullParser.START_TAG, ns, "feed")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name: String = parser.name
            // Starts by looking for the <entry> tag. This tag repeates inside of <feed> for each
            // article in the feed.
            //
            // Example:
            // <entry>
            //   <title>Article title</title>
            //   <link rel="alternate" type="text/html" href="http://example.com/article/1234"/>
            //   <link rel="edit" href="http://example.com/admin/article/1234"/>
            //   <id>urn:uuid:218AC159-7F68-4CC6-873F-22AE6017390D</id>
            //   <published>2003-06-27T12:00:00Z</published>
            //   <updated>2003-06-28T12:00:00Z</updated>
            //   <summary>Article summary goes here.</summary>
            //   <author>
            //     <name>Rick Deckard</name>
            //     <email>deckard@example.com</email>
            //   </author>
            // </entry>
            if (name == "entry") {
                entries.add(readEntry(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    /**
     * Parses the contents of an entry. If it encounters a "title", "summary", or "link" tag, hands
     * them off to their respective "read" methods for processing. Otherwise, skips the tag.
     *
     * First we check if the current event is a `START_TAG` with the name "entry" (throwing
     * [XmlPullParserException] if it is not). Then we initialize [String] variable `var id`,
     * [String] variable `var title`, and [String] variable `var link` to `null` and [Long] variable
     * `var publishedOn` to 0.
     *
     * We then loop while the next event of [parser] is not `END_TAG`. If the event type is not
     * START_TAG we skip the event, otherwise we set [String] variable `val name` to the name of
     * the current element and execute a `when` statement on `name`:
     *
     *  * "id" - we set `id` to the value that our method [readTag] returns when reading [parser]
     *  for a [TAG_ID] element.
     *
     *  * "title" - we set `title` to the value that our method [readTag] returns when reading
     *  [parser] for a [TAG_TITLE] element.
     *
     *  * "link" - we set [String] variable `val tempLink` to the value that our method [readTag]
     *  returns when reading [parser] for a [TAG_LINK] element, then if `tempLink` is not `null` we
     *  set `link` to `tempLink`.
     *
     *  * "published" - we initialize [Time] variable `val t` with a new instance, call the
     *  [Time.parse3339] method of `t` with the value that our method [readTag] returns when
     *  reading [parser] for a TAG_PUBLISHED element, then set `publishedOn` to `t` converted to
     *  milliseconds.
     *
     *  * `else` - we skip this tag since we are not interested in it.
     *
     * When we reach the `END_TAG` of this "entry" element, we return a new instance of [Entry]
     * constructed using `id`, `title`, `link`, and `publishedOn`.
     *
     * @param parser Incoming XMl
     * @return [Entry] object created from the "entry" element read from [parser]
     * @throws XmlPullParserException on error parsing feed.
     * @throws IOException on I/O error.
     * @throws ParseException Error has been reached unexpectedly while parsing.
     */
    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    private fun readEntry(parser: XmlPullParser): Entry {
        parser.require(XmlPullParser.START_TAG, ns, "entry")
        var id: String? = null
        var title: String? = null
        var link: String? = null
        var publishedOn: Long = 0
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                "id" ->                     // Example: <id>urn:uuid:218AC159-7F68-4CC6-873F-22AE6017390D</id>
                    id = readTag(parser, TAG_ID)

                "title" ->                     // Example: <title>Article title</title>
                    title = readTag(parser, TAG_TITLE)

                "link" -> {
                    // Example: <link rel="alternate" type="text/html" href="http://example.com/article/1234"/>
                    //
                    // Multiple link types can be included. readAlternateLink() will only return
                    // non-null when reading an "alternate"-type link. Ignore other responses.
                    val tempLink = readTag(parser, TAG_LINK)
                    if (tempLink != null) {
                        link = tempLink
                    }
                }

                "published" -> {
                    // Example: <published>2003-06-27T12:00:00Z</published>
                    val t = Time()
                    t.parse3339(readTag(parser, TAG_PUBLISHED))
                    publishedOn = t.toMillis(false)
                }

                else -> skip(parser)
            }
        }
        return Entry(id, title, link, publishedOn)
    }

    /**
     * Process an incoming tag and read the selected value from it. We switch on our [Int] parameter
     * [tagType]:
     *
     *  * [TAG_ID] - we return the [String] returned by our method [readBasicTag] when it reads
     *  [parser] looking for an "id" element.
     *
     *  * [TAG_TITLE] - we return the [String] returned by our method [readBasicTag] when it reads
     *  [parser] looking for a "title" element.
     *
     *  * [TAG_PUBLISHED] - we return the [String] returned by our method [readBasicTag] when it
     *  reads [parser] looking for a "published" element.
     *
     *  * [TAG_LINK] - we return the [String] returned by our method [readAlternateLink] when it
     *  reads [parser].
     *
     *  * `else` - we throw [IllegalArgumentException]
     *
     * @param parser the [XmlPullParser] we are to read from
     * @param tagType Type of the tag we are supposed to read
     * @return Body of the specified tag
     * @throws IOException            on I/O error.
     * @throws XmlPullParserException on error parsing feed.
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTag(parser: XmlPullParser, tagType: Int): String? {
        return when (tagType) {
            TAG_ID -> readBasicTag(parser, "id")
            TAG_TITLE -> readBasicTag(parser, "title")
            TAG_PUBLISHED -> readBasicTag(parser, "published")
            TAG_LINK -> readAlternateLink(parser)
            else -> throw IllegalArgumentException("Unknown tag type: $tagType")
        }
    }

    /**
     * Reads the body of a basic XML tag, which is guaranteed not to contain any nested elements.
     * You probably want to call [readTag]. First we check if the current event is a START_TAG with
     * the name [tag] (throwing [XmlPullParserException] if it is not). We set [String] variable
     * `val result` to the value returned by our method [readText] when reading from [parser]. Then
     * we check if the current event is a END_TAG with the name [tag] (throwing [XmlPullParserException]
     * if it is not). Finally we return `result` to the caller.
     *
     * @param parser Current parser object
     * @param tag XML element tag name to parse
     * @return Body of the specified tag
     * @throws [IOException] Signals that an I/O exception of some sort has occurred. This class
     * is the general class of exceptions produced by failed or interrupted I/O operations.
     * @throws [XmlPullParserException] This exception is thrown to signal XML Pull Parser
     * related faults.
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readBasicTag(parser: XmlPullParser, tag: String): String? {
        parser.require(XmlPullParser.START_TAG, ns, tag)
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, tag)
        return result
    }

    /**
     * Processes link tags in the feed. First we initialize [String] variable `var link` to `null`.
     * Then we check if the current event is a START_TAG with the name "link" (throwing
     * [XmlPullParserException] if it is not). Then we call the `getName` method of [parser] (a
     * copy/paste residue?), and set [String] variable `val relType` to the value of the attribute
     * "rel" if any. If `relType` is "alternate", we set `link` to the value of the attribute "href"
     * if any.
     *
     * Then we loop until the next event is END_TAG, and then return `link` to the caller.
     *
     * @param parser Current parser object
     * @return Body of the "link" tag
     * @throws [IOException] Signals that an I/O exception of some sort has occurred. This class is
     * the general class of exceptions produced by failed or interrupted I/O operations.
     * @throws [XmlPullParserException] This exception is thrown to signal XML Pull Parser
     * related faults.
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAlternateLink(parser: XmlPullParser): String? {
        var link: String? = null
        parser.require(XmlPullParser.START_TAG, ns, "link")
        parser.name // Is this really necessary?
        val relType = parser.getAttributeValue(null, "rel")
        if (relType == "alternate") {
            link = parser.getAttributeValue(null, "href")
        }
        while (true) {
            if (parser.nextTag() == XmlPullParser.END_TAG) break
            // Intentionally break; consumes any remaining sub-tags.
        }
        return link
    }

    /**
     * For the tags "title" and "summary", extracts their text values. First we initialize the
     * [String] variable `var result` to `null`. If the next parsing event is `TEXT` we set `result`
     * to the element content, and advance [parser] to the next event (`START_TAG` or `END_TAG`).
     * Finally we return `result` to the caller.
     *
     * @param parser Current parser object
     * @return text content of TEXT event (if any)
     * @throws IOException Signals that an I/O exception of some sort has occurred. This class is
     * the general class of exceptions produced by failed or interrupted I/O operations.
     * @throws XmlPullParserException This exception is thrown to signal XML Pull Parser related faults.
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String? {
        var result: String? = null
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    /**
     * Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e., if the
     * next tag after a `START_TAG` isn't a matching `END_TAG`, it keeps going until it finds the
     * matching `END_TAG` (as indicated by the value of "depth" being 0).
     *
     * First we fetch the type of the current event of [parser] and throw [IllegalStateException]
     * if it is not a `START_TAG`. Then we initialize [Int] variable `var depth` to 1, then loop
     * while depth is not equal to 0. We `when` switch on the next event:
     *
     *  * `END_TAG` - we decrement depth
     *
     *  * `START_TAG` - we increment depth
     *
     * @param parser Incoming XMl
     * @throws XmlPullParserException This exception is thrown to signal XML Pull Parser related faults.
     * @throws IOException Signals that an I/O exception of some sort has occurred. This class is
     * the general class of exceptions produced by failed or interrupted I/O operations.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    /**
     * This class represents a single entry (post) in the XML feed. It includes the data members
     * "id", "title," "link," and "published"
     */
    class Entry
    /**
     * Our constructor, we simply save our parameters in their respective fields.
     *
     * @param id        Value of the "id" element.
     * @param title     Value of the "title" element.
     * @param link      Value of the "link" element.
     * @param published Value of the "published" element, converted to milliseconds.
     */
    internal constructor(
        /**
         * Value of the "id" element.
         */
        val id: String?,
        /**
         * Value of the "title" element.
         */
        val title: String?,
        /**
         * Value of the "link" element.
         */
        val link: String?,
        /**
         * Value of the "published" element, converted to milliseconds.
         */
        val published: Long
    )

    companion object {
        // Constants indicting XML element names that we're interested in

        /**
         * "id" element
         */
        private const val TAG_ID = 1

        /**
         * "title" element
         */
        private const val TAG_TITLE = 2

        /**
         * "published" element
         */
        private const val TAG_PUBLISHED = 3

        /**
         * "link" element
         */
        private const val TAG_LINK = 4

        /**
         * Xml namespace. We don't use XML namespaces, `null` will match any namespace
         */
        private val ns: String? = null
    }
}