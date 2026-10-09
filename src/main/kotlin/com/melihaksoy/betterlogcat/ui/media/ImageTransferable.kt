package com.melihaksoy.betterlogcat.ui.media

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

internal class ImageTransferable(private val image: Image) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor != DataFlavor.imageFlavor) throw UnsupportedFlavorException(flavor)
        return image
    }
}
