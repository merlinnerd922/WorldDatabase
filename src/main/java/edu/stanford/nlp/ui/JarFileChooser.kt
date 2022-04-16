package edu.stanford.nlp.ui

import utils.matches
import java.awt.Frame
import javax.swing.JPanel
import javax.swing.JOptionPane
import javax.swing.JDialog
import javax.swing.JList
import javax.swing.ListSelectionModel
import javax.swing.JScrollPane
import javax.swing.JButton
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Point
import java.awt.event.*
import java.io.File
import kotlin.Throws
import java.util.zip.ZipException
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.zip.ZipFile

/**
 * This class shows a dialog which lets the user select a file from
 * among a list of files contained in a given jar file.  (This should
 * work for zip files as well, actually.)
 *
 * @author John Bauer
 */
class JarFileChooser(var pattern: String, var panel: JPanel) {
    var frame: Frame? = null
    fun show(filename: String?, location: Point?): String? {
        val jarFile = File(filename!!)
        if (!jarFile.exists()) {
            JOptionPane.showMessageDialog(panel, "Filename $jarFile does not exist", null, JOptionPane.ERROR_MESSAGE)
            return null
        }
        val files: List<String> = try {
            getFiles(jarFile)
        } catch (e: Exception) {
            // Something went wrong reading the file.
            JOptionPane.showMessageDialog(panel, "Filename $jarFile had an error:\n$e", null, JOptionPane.ERROR_MESSAGE)
            return null
        }
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(
                panel,
                "Filename $jarFile does not contain any models",
                null,
                JOptionPane.ERROR_MESSAGE
            )
            return null
        }
        return showListSelectionDialog(files, location)
    }

    private fun showListSelectionDialog(files: List<String>, location: Point?): String? {
        val frame = Frame()
        //System.out.println(location);
        //frame.setLocation(location);
        val dialog = JDialog(frame, "Jar File Chooser", true)
        dialog.location = location!!
        val fileList = JList(Vector(files))
        fileList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val mouseListener: MouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    // double-clicked
                    dialog.isVisible = false
                }
            }
        }
        fileList.addMouseListener(mouseListener)
        val scroll = JScrollPane(fileList)
        val okay = JButton()
        okay.text = "Okay"
        okay.toolTipText = "Okay"
        okay.addActionListener { dialog.isVisible = false }
        val cancel = JButton()
        cancel.text = "Cancel"
        cancel.toolTipText = "Cancel"
        cancel.addActionListener {
            fileList.clearSelection()
            dialog.isVisible = false
        }
        val gridbag = GridBagLayout()
        val constraints = GridBagConstraints()
        dialog.layout = gridbag
        constraints.gridwidth = GridBagConstraints.REMAINDER
        constraints.fill = GridBagConstraints.BOTH
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        gridbag.setConstraints(scroll, constraints)
        dialog.add(scroll)
        constraints.gridwidth = GridBagConstraints.RELATIVE
        constraints.fill = GridBagConstraints.NONE
        constraints.weighty = 0.0
        gridbag.setConstraints(okay, constraints)
        dialog.add(okay)
        constraints.gridwidth = GridBagConstraints.REMAINDER
        gridbag.setConstraints(cancel, constraints)
        dialog.add(cancel)
        dialog.pack()
        dialog.size = dialog.preferredSize
        dialog.isVisible = true
        return if (fileList.isSelectionEmpty) null else files[fileList.selectedIndex]
    }

    @Throws(ZipException::class, IOException::class)
    fun getFiles(jarFile: File?): List<String> {
        //System.out.println("Looking at " + jarFile);
        val files: MutableList<String> = ArrayList()
        ZipFile(jarFile!!).use { zin ->
            val entries = zin.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.matches(pattern)) {
                    files.add(name)
                }
            }
            files.sort()
        }
        return files
    }
}

